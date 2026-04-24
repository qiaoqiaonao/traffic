package com.traffic.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class TaskResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String taskId;
    private String status;  // processing, completed, failed
    private Map<String, Object> videoInfo;
    private Map<String, Object> statistics;
    private Object frameResults;  // 详细结果（可能很大）
    private String error;

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getVideoInfo() { return videoInfo; }
    public void setVideoInfo(Map<String, Object> videoInfo) { this.videoInfo = videoInfo; }

    public Map<String, Object> getStatistics() { return statistics; }
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }

    public Object getFrameResults() { return frameResults; }
    public void setFrameResults(Object frameResults) { this.frameResults = frameResults; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}