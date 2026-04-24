// src/main/java/com/traffic/service/AsyncAnalyzeService.java
package com.traffic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.entity.AnalyzeTask;
import com.traffic.enums.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Service
@Slf4j
public class AsyncAnalyzeService {

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private TaskDbService taskDbService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FileStorageService fileStorageService;

    // 用于存储正在运行的任务
    private final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> analyzeFrame(MultipartFile file) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceUrl + "/api/analyze/frame",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            return data;

        } catch (Exception e) {
            log.error("单帧检测失败", e);
            throw new RuntimeException("检测失败: " + e.getMessage());
        }
    }

    private final ConcurrentHashMap<String, Boolean> cancelFlags = new ConcurrentHashMap<>();

    @Async("taskExecutor")
    public Future<Boolean> submitAnalyzeTask(String taskId, String filePath,
                                             Integer frameSkip, String detectionLinesJson) {

        // 初始化取消标志
        cancelFlags.put(taskId, false);

        Future<Boolean> future = new AsyncResult<>(true);
        runningTasks.put(taskId, future);
        log.info("开始异步任务: taskId={}", taskId);

        try {
            taskDbService.updateProgress(taskId, 5);
            webSocketService.sendProgress(taskId, 5, "准备上传视频...");

            Path videoFile = Paths.get(filePath);
            if (!Files.exists(videoFile)) {
                throw new RuntimeException("视频文件不存在: " + filePath);
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("task_id", taskId);
            body.add("file", new org.springframework.core.io.FileSystemResource(videoFile.toFile()));
            body.add("frame_skip", frameSkip);
            if (detectionLinesJson != null) {
                body.add("detection_lines", detectionLinesJson);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            webSocketService.sendProgress(taskId, 10, "上传到AI服务...");

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceUrl + "/api/analyze/upload",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> aiResponse = response.getBody();
            if (aiResponse == null || !Integer.valueOf(200).equals(aiResponse.get("code"))) {
                throw new RuntimeException("AI服务错误: " + aiResponse);
            }

            log.info("AI服务响应: {}", aiResponse);

            // 轮询结果 - 传入taskId以便检查取消状态
            pollAndUpdate(taskId);

            return new AsyncResult<>(true);

        }  catch (InterruptedException e) {
            log.info("任务被中断: taskId={}", taskId);
            // 取消时数据库状态已经更新，不需要再调用 handleFailure
            webSocketService.sendProgress(taskId, -1, "已取消");
            Thread.currentThread().interrupt();
            return new AsyncResult<>(false);
        } catch (Exception e) {
            log.error("任务失败: taskId={}", taskId, e);
            String errorMsg = e.getMessage();
            // 检查是否是取消导致的（通过cancelFlags判断更准确）
            if (Boolean.TRUE.equals(cancelFlags.get(taskId))) {
                log.info("检测到取消标志，使用取消流程处理: taskId={}", taskId);
                webSocketService.sendProgress(taskId, -1, "已取消");
            } else {
                handleFailure(taskId, errorMsg);
            }
            return new AsyncResult<>(false);
        } finally {
            runningTasks.remove(taskId);
            cancelFlags.remove(taskId);
            fileStorageService.deleteFile(filePath);
        }
    }


    public boolean cancelTask(String taskId) {
        // 设置取消标志
        cancelFlags.put(taskId, true);

        Future<?> future = runningTasks.get(taskId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true); // true表示中断执行中的线程
            if (cancelled) {
                log.info("任务已取消: {}", taskId);
            }
            return cancelled;
        }

        AnalyzeTask task = taskDbService.getByTaskId(taskId);
        if (task != null && task.getStatus() == TaskStatus.PROCESSING.getCode()) {
            taskDbService.failTask(taskId, "用户取消");
            return true;
        }

        return false;
    }

    private void pollAndUpdate(String taskId) throws InterruptedException {
        int baseDelay = 2000;
        int maxDelay = 10000;
        int maxAttempts = 300;
        int attempt = 0;

        while (attempt < maxAttempts) {
            if (Boolean.TRUE.equals(cancelFlags.get(taskId))) {
                throw new InterruptedException("用户取消");
            }

            try {
                long delay = (long) baseDelay * (1L << (attempt / 10));
                Thread.sleep(Math.min(delay, maxDelay));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }

            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(
                        aiServiceUrl + "/api/analyze/result/" + taskId, Map.class);

                Map<String, Object> result = response.getBody();
                if (result == null || !Integer.valueOf(200).equals(result.get("code"))) {
                    attempt++;
                    continue;
                }

                Map<String, Object> data = (Map<String, Object>) result.get("data");
                String status = (String) data.get("status");
                Integer progress = (Integer) data.get("progress");

                webSocketService.sendProgress(taskId, progress, (String) data.get("message"));

                if ("completed".equals(status)) {
                    Map<String, Object> resultData = (Map<String, Object>) data.get("result");

                    // ✅ 关键：如果保存失败，直接抛异常结束轮询，别无限重试
                    try {
                        taskDbService.completeTask(taskId, resultData);
                        webSocketService.sendProgress(taskId, 100, "分析完成！");
                        return; // 成功，结束轮询
                    } catch (Exception saveEx) {
                        log.error("保存结果失败，终止任务: taskId={}, error={}", taskId, saveEx.getMessage());
                        throw new RuntimeException("保存结果失败: " + saveEx.getMessage());
                    }

                } else if ("failed".equals(status)) {
                    String error = (String) data.get("error");
                    throw new RuntimeException("AI分析失败: " + error);
                } else if ("cancelled".equals(status)) {
                    throw new InterruptedException("用户取消");
                }

                attempt++;

            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                // 区分：网络抖动可重试；业务失败（AI 报错、保存失败）立即终止轮询
                String msg = e.getMessage();
                if (msg != null && (msg.contains("保存结果失败") || msg.contains("AI分析失败"))) {
                    throw new RuntimeException(msg);
                }
                log.warn("轮询网络异常，继续重试: {}", msg);
                attempt++;
            }
        }
        throw new RuntimeException("轮询超时");
    }

    private void handleFailure(String taskId, String error) {
        webSocketService.sendProgress(taskId, -1, "失败: " + error);
        taskDbService.failTask(taskId, error);
    }

    public Map<String, Object> getDetailedResult(String taskId) {
        try {
            String cached = taskService.getResultJson(taskId);
            if (cached != null && !cached.trim().isEmpty()) {
                return objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
            }
            AnalyzeTask fromDb = taskDbService.getByTaskId(taskId);
            if (fromDb != null && fromDb.getResultJson() != null && !fromDb.getResultJson().trim().isEmpty()) {
                taskService.saveResultJson(taskId, fromDb.getResultJson());
                return objectMapper.readValue(fromDb.getResultJson(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("从 Redis/MySQL 读取详细结果失败，回源 AI: {}", e.getMessage());
        }

        ResponseEntity<Map> response = restTemplate.getForEntity(
                aiServiceUrl + "/api/analyze/result/" + taskId,
                Map.class
        );

        if (response.getBody() == null || !Integer.valueOf(200).equals(response.getBody().get("code"))) {
            throw new RuntimeException("AI 服务返回异常");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        if (data == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) data.get("result");
        if (result != null) {
            try {
                taskService.saveResultJson(taskId, objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                log.warn("回源 AI 后写入 Redis 失败: taskId={}", taskId, e);
            }
        }
        return result;
    }
}