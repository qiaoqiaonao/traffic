// src/main/java/com/traffic/controller/TrafficController.java
package com.traffic.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.entity.AnalyzeTask;
//import com.traffic.entity.PageResult;
import com.traffic.enums.TaskStatus;
import com.traffic.service.*;
import com.traffic.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.*;

@RestController
@RequestMapping("/traffic")
@Validated
@Slf4j
public class TrafficController {

    @Autowired
    private AsyncAnalyzeService asyncAnalyzeService;
    @Autowired
    private TaskDbService taskDbService;
    @Autowired
    private WebSocketService webSocketService;
    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    /**
     * 上传视频分析
     */
    @Value("${traffic.violation.meters-per-pixel:0.05}")
    private double defaultMetersPerPixel;

    @PostMapping("/analyze")
    public ApiResponse<Map<String, Object>> analyzeVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "frameSkip", defaultValue = "3") @Min(1) @Max(10) Integer frameSkip,
            @RequestParam(value = "detectionLines", required = false) String detectionLinesJson,
            @RequestParam(value = "metersPerPixel", defaultValue = "0.05") double metersPerPixel) {

        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 使用 default 当没传或传 0 时
        if (metersPerPixel <= 0) metersPerPixel = defaultMetersPerPixel;

        try {
            // 保存文件
            String filePath = fileStorageService.saveFile(file, taskId);

            // 创建数据库记录
            taskDbService.createTask(taskId, file.getOriginalFilename(), filePath,
                    file.getSize(), frameSkip);

            // 启动异步任务
            asyncAnalyzeService.submitAnalyzeTask(taskId, filePath, frameSkip,
                    detectionLinesJson, metersPerPixel);

            // 返回响应
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", TaskStatus.PROCESSING.getCode());
            result.put("wsUrl", "/ws/progress?taskId=" + taskId);
            result.put("checkUrl", "/api/traffic/result/" + taskId);

            return ApiResponse.success(result);

        } catch (IllegalArgumentException e) {
            log.warn("参数错误: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("提交失败: taskId={}", taskId, e);
            taskDbService.failTask(taskId, e.getMessage());
            return ApiResponse.error(500, "提交失败: " + e.getMessage());
        }
    }

    /**
     * 查询分析结果
     */
    @GetMapping("/result/{taskId}")
    public ApiResponse<AnalyzeTask> getResult(@PathVariable String taskId) {
        AnalyzeTask task = taskDbService.getByTaskId(taskId);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在");
        }

        // 处理中状态获取实时进度
        if (task.getStatus() == TaskStatus.PROCESSING.getCode()) {
            Integer progress = webSocketService.getProgress(taskId);
            if (progress != null) {
                task.setCurrentProgress(progress);
            }
        }

        // ✅ 任务已成功完成但 resultJson 为空时，向 AI 端补录（失败任务不补录，避免无效请求）
        if (task.getStatus() == TaskStatus.COMPLETED.getCode()
                && (task.getResultJson() == null || task.getResultJson().isEmpty())) {

            try {
                log.info("数据库结果为空，实时补录: taskId={}", taskId);

                // 实时去 AI 端获取完整结果
                Map<String, Object> aiResult = asyncAnalyzeService.getDetailedResult(taskId);

                if (aiResult != null && !aiResult.isEmpty()) {
                    String resultJson = objectMapper.writeValueAsString(aiResult);
                    task.setResultJson(resultJson);

                    // 异步回补数据库（防止下次再补）；updateResultJson 内会同步写入 Redis
                    taskDbService.updateResultJson(taskId, resultJson);

                    log.info("实时补录成功: taskId={}", taskId);
                }
            } catch (Exception e) {
                log.error("实时补录失败: taskId={}, error={}", taskId, e.getMessage());
                // 补录失败不影响返回，前端至少能拿到实时数据
            }
        }

        return ApiResponse.success(task);
    }

    /**
     * 获取详细结果
     */
    @GetMapping("/result/{taskId}/detail")
    public ApiResponse<Map<String, Object>> getDetailedResult(@PathVariable String taskId) {
        try {
            Map<String, Object> detail = asyncAnalyzeService.getDetailedResult(taskId);
            return ApiResponse.success(detail);
        } catch (Exception e) {
            log.error("获取详细结果失败: {}", taskId, e);
            return ApiResponse.error(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 单帧检测
     */
    @PostMapping("/analyze/frame")
    public ApiResponse<Map<String, Object>> analyzeFrame(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ApiResponse.error(400, "请选择图片文件");
            }

            String originalName = file.getOriginalFilename();
            String ext = originalName != null ?
                    originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase() : "";

            Set<String> allowed = Set.of("jpg", "jpeg", "png", "bmp", "webp");
            if (!allowed.contains(ext)) {
                return ApiResponse.error(400, "不支持的图片格式: " + ext);
            }

            if (file.getSize() > 10 * 1024 * 1024) { // 10MB限制
                return ApiResponse.error(400, "图片大小不能超过10MB");
            }

            Map<String, Object> result = asyncAnalyzeService.analyzeFrame(file);
            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("单帧检测失败", e);
            return ApiResponse.error(500, "检测失败: " + e.getMessage());
        }
    }

    /**
     * 历史记录---无分页
     */
    /*@GetMapping("/history")
    public ApiResponse<List<AnalyzeTask>> getHistory(
            @RequestParam(defaultValue = "10") @Max(100) Integer limit,
            @RequestParam(required = false) Integer status) {

        List<AnalyzeTask> list = taskDbService.getRecentTasks(limit, status);

        // 确保状态正确映射
        for (AnalyzeTask task : list) {
            // 如果进度100但状态不是完成，修正它
            if (task.getProgress() != null && task.getProgress() == 100
                    && task.getStatus() != TaskStatus.COMPLETED.getCode()) {
                log.warn("修正状态不一致: taskId={}, status={}, progress=100",
                        task.getTaskId(), task.getStatus());
                task.setStatus(TaskStatus.COMPLETED.getCode());
            }
        }

        return ApiResponse.success(list);
    }*/

    /**
     * 历史记录
     */
    @GetMapping("/history")
    public ApiResponse<Page<AnalyzeTask>> getHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {

        Page<AnalyzeTask> pageResult = taskDbService.getTasksByPage(page, pageSize, status);
        return ApiResponse.success(pageResult);
    }

    /**
     * 取消任务
     */
    @PostMapping("/cancel/{taskId}")
    public ApiResponse<Void> cancelTask(@PathVariable String taskId) {
        try {
            log.info("收到取消任务请求: {}", taskId);

            AnalyzeTask task = taskDbService.getByTaskId(taskId);
            if (task == null) {
                return ApiResponse.error(404, "任务不存在");
            }

            // 只能取消等待中或进行中的任务
            if (task.getStatus() == TaskStatus.COMPLETED.getCode()) {
                return ApiResponse.error(400, "任务已完成");
            }
            if (task.getStatus() == TaskStatus.FAILED.getCode()) {
                return ApiResponse.error(400, "任务已失败");
            }

            // 更新状态为取消（使用失败状态表示）
            taskDbService.cancelTask(taskId);

            // 尝试取消正在运行的任务（如果已经在处理）
            asyncAnalyzeService.cancelTask(taskId);

            // 通知 Python 端停止处理（可选）
            try {
                restTemplate.postForEntity(
                        aiServiceUrl + "/api/analyze/cancel/" + taskId,
                        null,
                        String.class
                );
            } catch (Exception e) {
                log.warn("通知 Python 取消失败（可能还未开始处理）: {}", e.getMessage());
            }

            return ApiResponse.success(null);

        } catch (Exception e) {
            log.error("取消任务失败: {}", taskId, e);
            return ApiResponse.error(500, "取消失败: " + e.getMessage());
        }
    }

    /**
     * 违规记录明细查询
     */
    @GetMapping("/violations/{taskId}")
    public ApiResponse<Map<String, Object>> getViolations(@PathVariable String taskId) {
        AnalyzeTask task = taskDbService.getByTaskId(taskId);
        if (task == null) {
            return ApiResponse.error(404, "任务不存在");
        }
        if (task.getResultJson() == null || task.getResultJson().isEmpty()) {
            return ApiResponse.success(Map.of("total", 0, "details", List.of()));
        }

        try {
            Map<String, Object> fullResult = objectMapper.readValue(
                    task.getResultJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            Map<String, Object> statistics = (Map<String, Object>) fullResult.get("statistics");
            Map<String, Object> violations = statistics != null ?
                    (Map<String, Object>) statistics.get("violations") : null;

            Map<String, Object> result = new HashMap<>();
            result.put("total", violations != null ? violations.get("total") : 0);
            result.put("details", violations != null ? violations.get("details") : List.of());
            result.put("summary", Map.of(
                    "wrongDirectionCount", task.getWrongDirectionCount() != null ? task.getWrongDirectionCount() : 0,
                    "illegalParkingCount", task.getIllegalParkingCount() != null ? task.getIllegalParkingCount() : 0,
                    "speedingCount", task.getSpeedingCount() != null ? task.getSpeedingCount() : 0,
                    "congestionCount", task.getCongestionCount() != null ? task.getCongestionCount() : 0
            ));
            result.put("maxSpeedKmh", task.getMaxSpeedKmh());
            result.put("avgSpeedKmh", task.getAvgSpeedKmh());

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取违规明细失败: {}", taskId, e);
            return ApiResponse.error(500, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 全局违规统计
     */
    @GetMapping("/violations/stats")
    public ApiResponse<Map<String, Object>> getViolationStats(
            @RequestParam(defaultValue = "100") @Max(500) Integer limit) {

        List<AnalyzeTask> tasks = taskDbService.getRecentTasks(limit, TaskStatus.COMPLETED.getCode());

        int wrongDirection = 0, illegalParking = 0, speeding = 0, congestion = 0;
        double maxSpeedAll = 0, speedSum = 0;
        int speedCount = 0;

        for (AnalyzeTask t : tasks) {
            if (t.getWrongDirectionCount() != null) wrongDirection += t.getWrongDirectionCount();
            if (t.getIllegalParkingCount() != null) illegalParking += t.getIllegalParkingCount();
            if (t.getSpeedingCount() != null) speeding += t.getSpeedingCount();
            if (t.getCongestionCount() != null) congestion += t.getCongestionCount();
            if (t.getMaxSpeedKmh() != null && t.getMaxSpeedKmh().doubleValue() > maxSpeedAll) {
                maxSpeedAll = t.getMaxSpeedKmh().doubleValue();
            }
            if (t.getAvgSpeedKmh() != null) {
                speedSum += t.getAvgSpeedKmh().doubleValue();
                speedCount++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("wrongDirectionCount", wrongDirection);
        data.put("illegalParkingCount", illegalParking);
        data.put("speedingCount", speeding);
        data.put("congestionCount", congestion);
        data.put("totalViolations", wrongDirection + illegalParking + speeding + congestion);
        data.put("maxSpeedKmh", maxSpeedAll > 0 ? maxSpeedAll : null);
        data.put("avgSpeedKmh", speedCount > 0 ? speedSum / speedCount : null);
        data.put("analyzedTasks", tasks.size());

        return ApiResponse.success(data);
    }
}