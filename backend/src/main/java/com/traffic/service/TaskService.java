package com.traffic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务分析结果缓存：优先使用 Redis（与开题报告「Redis 缓存高频数据」一致），
 * 不可用时降级为进程内 Map，避免单机无 Redis 时功能不可用。
 */
@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    private static final String KEY_PREFIX = "traffic:task:result:";

    private final StringRedisTemplate redisTemplate;

    @Value("${traffic.cache.task-result-ttl-hours:168}")
    private long taskResultTtlHours;

    private final Map<String, String> localFallback = new ConcurrentHashMap<>();

    public TaskService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String taskId) {
        return KEY_PREFIX + taskId;
    }

    public void saveResultJson(String taskId, String resultJson) {
        if (taskId == null || resultJson == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    key(taskId),
                    resultJson,
                    Duration.ofHours(Math.max(1, taskResultTtlHours))
            );
        } catch (Exception e) {
            logger.warn("Redis 写入任务结果失败，使用内存降级: taskId={}, err={}", taskId, e.getMessage());
            localFallback.put(taskId, resultJson);
        }
    }

    /**
     * @return JSON 字符串，未命中返回 null
     */
    public String getResultJson(String taskId) {
        if (taskId == null) {
            return null;
        }
        try {
            String v = redisTemplate.opsForValue().get(key(taskId));
            if (v != null && !v.isEmpty()) {
                return v;
            }
        } catch (Exception e) {
            logger.warn("Redis 读取任务结果失败，尝试内存: taskId={}, err={}", taskId, e.getMessage());
        }
        return localFallback.get(taskId);
    }

    public void delete(String taskId) {
        if (taskId == null) {
            return;
        }
        try {
            redisTemplate.delete(key(taskId));
        } catch (Exception e) {
            logger.warn("Redis 删除任务缓存失败: taskId={}, err={}", taskId, e.getMessage());
        }
        localFallback.remove(taskId);
    }
}
