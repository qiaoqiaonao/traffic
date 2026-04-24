// src/main/java/com/traffic/websocket/ProgressWebSocketHandler.java
package com.traffic.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ProgressWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private WebSocketService webSocketService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(1);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ProgressWebSocketHandler() {
        // 启动心跳检测
        heartbeatExecutor.scheduleAtFixedRate(this::checkHeartbeats, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String taskId = extractTaskId(session);

        if (taskId == null) {
            log.error("缺少taskId参数");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        sessions.put(session.getId(), session);
        webSocketService.registerSession(taskId, session);

        log.info("WebSocket连接: taskId={}, sessionId={}", taskId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            if ("pong".equals(data.get("type"))) {
                // 心跳响应
            }
        } catch (Exception e) {
            log.debug("消息处理失败: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String taskId = extractTaskId(session);
        sessions.remove(session.getId());

        if (taskId != null) {
            webSocketService.removeSession(taskId);
            log.info("WebSocket关闭: taskId={}, status={}", taskId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket错误: {}", exception.getMessage());
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    private String extractTaskId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query == null) return null;

        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && "taskId".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private void checkHeartbeats() {
        sessions.forEach((id, session) -> {
            if (!session.isOpen()) {
                log.warn("死连接清理: {}", id);
                sessions.remove(id);
            }
        });
    }
}