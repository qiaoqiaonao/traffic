package com.traffic.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TaskService {
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
            log.warn("Redis 写入任务结果失败，使用内存降级: taskId={}, err={}", taskId, e.getMessage());
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
            log.warn("Redis 读取任务结果失败，尝试内存: taskId={}, err={}", taskId, e.getMessage());
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
            log.warn("Redis 删除任务缓存失败: taskId={}, err={}", taskId, e.getMessage());
        }
        localFallback.remove(taskId);
    }
}
