// src/main/java/com/traffic/entity/AnalyzeTask.java
package com.traffic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("analyze_task")
public class AnalyzeTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String fileName;

    @JsonIgnore
    private String filePath;

    private Long fileSize;
    private Integer status;  // 0待处理, 1处理中, 2完成, 3失败
    private Integer progress;
    private Integer frameSkip;

    // 视频信息
    private Integer totalFrames;
    private BigDecimal fps;
    private BigDecimal durationSec;

    // 统计信息
    private Integer totalDetections;
    private BigDecimal avgCarsPerFrame;
    private Integer uniqueVehicles;

    // 结果存储（JSON格式）
    private String trafficSummary;  // 流量统计摘要
    private String violationsSummary;  // 违规摘要
    private String resultJson;  // 完整结果

    private String errorMsg;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime completedTime;

    // 非持久化字段
    @TableField(exist = false)
    private Integer currentProgress;
}