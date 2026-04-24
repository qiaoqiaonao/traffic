// src/main/java/com/traffic/service/WebSocketService.java
package com.traffic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WebSocketService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 本地Session管理
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Integer> progressCache = new ConcurrentHashMap<>();

    public void registerSession(String taskId, WebSocketSession session) {
        sessions.put(taskId, session);
        log.info("WebSocket注册: taskId={}, sessionId={}", taskId, session.getId());

        // 发送当前进度
        Integer progress = progressCache.get(taskId);
        if (progress != null) {
            sendProgress(taskId, progress, "连接成功");
        }
    }

    public void removeSession(String taskId) {
        sessions.remove(taskId);
        log.info("WebSocket注销: taskId={}", taskId);
    }

    public Integer getProgress(String taskId) {
        return progressCache.get(taskId);
    }

    public void sendProgress(String taskId, int progress, String message) {
        sendProgress(taskId, progress, message, null, null);
    }

    public void sendProgress(String taskId, int progress, String message,
                             Integer frameCount, Integer totalCars) {
        progressCache.put(taskId, progress);

        Map<String, Object> payload = Map.of(
                "taskId", taskId,
                "progress", progress,
                "message", message,
                "frameCount", frameCount != null ? frameCount : 0,
                "totalCars", totalCars != null ? totalCars : 0,
                "timestamp", System.currentTimeMillis()
        );

        // 发送到本地Session
        WebSocketSession session = sessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
                log.debug("发送进度: taskId={}, progress={}", taskId, progress);
            } catch (IOException e) {
                log.error("发送失败: {}", taskId, e);
                removeSession(taskId);
            }
        }

        // 发布到Redis（集群支持）
        try {
            redisTemplate.convertAndSend("traffic:progress:" + taskId,
                    objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("Redis发布失败: {}", e.getMessage());
        }
    }

    public void clearTask(String taskId) {
        sessions.remove(taskId);
        progressCache.remove(taskId);
    }
}