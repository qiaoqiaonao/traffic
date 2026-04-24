// src/main/java/com/traffic/enums/TaskStatus.java
package com.traffic.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDING(0, "等待中"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "完成"),
    FAILED(3, "失败");

    private final int code;
    private final String desc;

    TaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;

    }

    // 添加前端显示用的状态文本
    public String getLabel() {
        switch (this) {
            case PENDING: return "等待中";
            case PROCESSING: return "处理中";
            case COMPLETED: return "完成";
            case FAILED: return "失败";
            default: return "未知";
        }
    }

    public String getType() {
        switch (this) {
            case PENDING: return "info";
            case PROCESSING: return "warning";
            case COMPLETED: return "success";
            case FAILED: return "danger";
            default: return "info";
        }
    }
    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : values()) {
            if (status.code == code) return status;
        }
        return PENDING;
    }
}