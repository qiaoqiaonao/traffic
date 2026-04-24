// src/main/java/com/traffic/service/TaskDbService.java
package com.traffic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.entity.AnalyzeTask;
import com.traffic.entity.PageResult;
import com.traffic.enums.TaskStatus;
import com.traffic.mapper.AnalyzeTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TaskDbService extends ServiceImpl<AnalyzeTaskMapper, AnalyzeTask> {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnalyzeTaskMapper analyzeTaskMapper;

    @Autowired
    private TaskService taskService;

    @Transactional
    public AnalyzeTask createTask(String taskId, String fileName, String filePath,
                                  Long fileSize, Integer frameSkip) {
        AnalyzeTask task = AnalyzeTask.builder()
                .taskId(taskId)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .status(TaskStatus.PENDING.getCode())
                .progress(0)
                .frameSkip(frameSkip)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        save(task);
        log.info("创建任务记录: taskId={}, status={}", taskId, task.getStatus());
        return task;
    }

    public AnalyzeTask getByTaskId(String taskId) {
        AnalyzeTask task = baseMapper.selectByTaskId(taskId);
        if (task != null) {
            // 确保状态一致性
            log.debug("查询任务: taskId={}, status={}, progress={}",
                    taskId, task.getStatus(), task.getProgress());
        }
        return task;
    }


    /**
     * 取消任务（将状态标记为失败）
     */
    @Transactional
    public void cancelTask(String taskId) {
        lambdaUpdate()
                .eq(AnalyzeTask::getTaskId, taskId)
                .set(AnalyzeTask::getStatus, TaskStatus.FAILED.getCode())
                .set(AnalyzeTask::getProgress, -1)
                .set(AnalyzeTask::getErrorMsg, "用户取消")
                .set(AnalyzeTask::getUpdateTime, LocalDateTime.now())
                .update();

        log.info("任务已取消: taskId={}", taskId);
    }

    @Transactional
    public void updateProgress(String taskId, Integer progress) {
        // 获取当前任务
        AnalyzeTask task = getByTaskId(taskId);
        if (task == null) return;

        Integer newStatus = task.getStatus();

        // 根据进度确定状态
        if (progress >= 100) {
            newStatus = TaskStatus.COMPLETED.getCode();
        } else if (progress < 0) {
            newStatus = TaskStatus.FAILED.getCode();
        } else if (progress > 0) {
            // 关键修复：只要进度>0，就标记为处理中
            newStatus = TaskStatus.PROCESSING.getCode();
        }

        // 每次进度变化都更新，但减少数据库写入频率可以优化
        lambdaUpdate()
                .eq(AnalyzeTask::getTaskId, taskId)
                .set(AnalyzeTask::getProgress, progress)
                .set(AnalyzeTask::getStatus, newStatus)
                .set(AnalyzeTask::getUpdateTime, LocalDateTime.now())
                .update();

        log.debug("更新进度: taskId={}, progress={}, status={}", taskId, progress, newStatus);
    }

    @Transactional
    public void completeTask(String taskId, java.util.Map<String, Object> result) {
        log.info("开始保存任务结果: taskId={}, resultKeys={}", taskId,
                result != null ? result.keySet() : "null");

        try {
            java.util.Map<String, Object> videoInfo =
                    (java.util.Map<String, Object>) result.get("video_info");
            java.util.Map<String, Object> statistics =
                    (java.util.Map<String, Object>) result.get("statistics");
            java.util.Map<String, Object> trafficCounts =
                    (java.util.Map<String, Object>) statistics.get("traffic_counts");
            java.util.Map<String, Object> violations =
                    (java.util.Map<String, Object>) statistics.get("violations");

            AnalyzeTask update = new AnalyzeTask();
            update.setTaskId(taskId);
            update.setStatus(TaskStatus.COMPLETED.getCode());
            update.setProgress(100);

            // ✅ 关键修复：所有字段做空值检查，防止 NPE
            if (videoInfo != null) {
                update.setTotalFrames(getIntValue(videoInfo, "total_frames"));
                update.setFps(getBigDecimalValue(videoInfo, "fps"));
                update.setDurationSec(getBigDecimalValue(videoInfo, "duration_sec"));
            }

            if (statistics != null) {
                update.setTotalDetections(getIntValue(statistics, "total_detections"));
                update.setAvgCarsPerFrame(getBigDecimalValue(statistics, "avg_cars_per_frame"));
                update.setUniqueVehicles(getIntValue(statistics, "unique_vehicles"));

                // traffic_counts 和 violations 可能为 null
                if (trafficCounts != null) {
                    update.setTrafficSummary(objectMapper.writeValueAsString(trafficCounts));
                }
                if (violations != null) {
                    update.setViolationsSummary(objectMapper.writeValueAsString(violations));
                }
            }

            // result_json 必须存（这是前端要的大 JSON）
            String resultJson = objectMapper.writeValueAsString(result);
            update.setResultJson(resultJson);
            update.setCompletedTime(LocalDateTime.now());
            update.setUpdateTime(LocalDateTime.now());

            baseMapper.updateByTaskId(update);

            taskService.saveResultJson(taskId, resultJson);

            // 验证更新
            AnalyzeTask verify = getByTaskId(taskId);
            log.info("任务完成更新成功: taskId={}, status={}, progress={}, hasResult={}",
                    taskId, verify.getStatus(), verify.getProgress(),
                    verify.getResultJson() != null);

        } catch (Exception e) {  // ✅ 捕获所有异常，不只是 JsonProcessingException
            log.error("completeTask 失败详情: taskId={}, error={}", taskId, e.getMessage(), e);
            throw new RuntimeException("保存结果失败", e);
        }
    }

    // 辅助方法：安全获取 Integer
    private Integer getIntValue(java.util.Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object val = map.get(key);
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 辅助方法：安全获取 BigDecimal
    private BigDecimal getBigDecimalValue(java.util.Map<String, Object> map, String key) {
        if (map == null) return BigDecimal.ZERO;
        Object val = map.get(key);
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return new BigDecimal(val.toString());
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }


    /**
     * 仅更新 result_json（用于实时补录后的异步回补）
     */
    @Transactional
    public void updateResultJson(String taskId, String resultJson) {
        try {
            lambdaUpdate()
                    .eq(AnalyzeTask::getTaskId, taskId)
                    .set(AnalyzeTask::getResultJson, resultJson)
                    .set(AnalyzeTask::getUpdateTime, LocalDateTime.now())
                    .update();
            taskService.saveResultJson(taskId, resultJson);
            log.info("回补 result_json 成功: taskId={}", taskId);
        } catch (Exception e) {
            log.error("回补 result_json 失败: taskId={}", taskId, e);
            // 回补失败不影响主流程
        }
    }

    @Transactional
    public void failTask(String taskId, String errorMsg) {
        lambdaUpdate()
                .eq(AnalyzeTask::getTaskId, taskId)
                .set(AnalyzeTask::getStatus, TaskStatus.FAILED.getCode())
                .set(AnalyzeTask::getProgress, -1)
                .set(AnalyzeTask::getErrorMsg, errorMsg)
                .set(AnalyzeTask::getUpdateTime, LocalDateTime.now())
                .update();

        taskService.delete(taskId);
        log.error("任务失败: taskId={}, error={}", taskId, errorMsg);
    }

    public List<AnalyzeTask> getRecentTasks(int limit, Integer status) {
        List<AnalyzeTask> list;
        if (status != null) {
            list = lambdaQuery()
                    .eq(AnalyzeTask::getStatus, status)
                    .orderByDesc(AnalyzeTask::getCreateTime)
                    .last("LIMIT " + limit)
                    .list();
        } else {
            list = baseMapper.selectRecent(limit);
        }

        // 调试日志
        for (AnalyzeTask task : list) {
            log.debug("历史记录: taskId={}, status={}, progress={}",
                    task.getTaskId(), task.getStatus(), task.getProgress());
        }

        return list;
    }

    public Page<AnalyzeTask> getTasksByPage(int page, int pageSize, Integer status) {
        Page<AnalyzeTask> pageParam = new Page<>(page, pageSize);

        LambdaQueryWrapper<AnalyzeTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AnalyzeTask::getStatus, status);
        }
        wrapper.orderByDesc(AnalyzeTask::getCreateTime);

        Page<AnalyzeTask> result = analyzeTaskMapper.selectPage(pageParam, wrapper);

        // 添加调试日志
        System.out.println("当前页: " + result.getCurrent());
        System.out.println("每页大小: " + result.getSize());
        System.out.println("总记录数: " + result.getTotal());
        System.out.println("总页数: " + result.getPages());
        System.out.println("当前页数据条数: " + result.getRecords().size());

        return result;
    }
}