// src/main/java/com/traffic/mapper/AnalyzeTaskMapper.java
package com.traffic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.traffic.entity.AnalyzeTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyzeTaskMapper extends BaseMapper<AnalyzeTask> {

    @Update("<script>" +
            "UPDATE analyze_task SET " +
            "<if test='task.status != null'>status=#{task.status},</if>" +
            "<if test='task.progress != null'>progress=#{task.progress},</if>" +
            "<if test='task.totalFrames != null'>total_frames=#{task.totalFrames},</if>" +
            "<if test='task.fps != null'>fps=#{task.fps},</if>" +
            "<if test='task.durationSec != null'>duration_sec=#{task.durationSec},</if>" +
            "<if test='task.totalDetections != null'>total_detections=#{task.totalDetections},</if>" +
            "<if test='task.avgCarsPerFrame != null'>avg_cars_per_frame=#{task.avgCarsPerFrame},</if>" +
            "<if test='task.uniqueVehicles != null'>unique_vehicles=#{task.uniqueVehicles},</if>" +
            "<if test='task.trafficSummary != null'>traffic_summary=#{task.trafficSummary},</if>" +
            "<if test='task.violationsSummary != null'>violations_summary=#{task.violationsSummary},</if>" +
            "<if test='task.resultJson != null'>result_json=#{task.resultJson},</if>" +
            "<if test='task.errorMsg != null'>error_msg=#{task.errorMsg},</if>" +
            "<if test='task.completedTime != null'>completed_time=#{task.completedTime},</if>" +
            "update_time=NOW() " +
            "WHERE task_id=#{task.taskId}" +
            "</script>")
    int updateByTaskId(@Param("task") AnalyzeTask task);

    @Select("SELECT * FROM analyze_task WHERE task_id = #{taskId}")
    AnalyzeTask selectByTaskId(@Param("taskId") String taskId);

    @Select("SELECT * FROM analyze_task ORDER BY create_time DESC LIMIT #{limit}")
    List<AnalyzeTask> selectRecent(@Param("limit") int limit);

    @Select("SELECT status, COUNT(*) as count FROM analyze_task GROUP BY status")
    List<Map<String, Object>> countByStatus();
}