package com.traffic.controller;

import com.traffic.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class HealthController {

    @GetMapping({"/traffic/health", "/api/video/health"})
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
                "status", "up",
                "service", "traffic-analysis-backend",
                "version", "2.0.0"
        ));
    }
}
