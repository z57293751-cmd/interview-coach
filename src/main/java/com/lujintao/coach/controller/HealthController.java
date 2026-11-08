package com.lujintao.coach.controller;

import com.lujintao.coach.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/ping")
    public ApiResult<Map<String, String>> ping() {
        return ApiResult.ok(Map.of("status", "ok", "time", LocalDateTime.now().toString()));
    }
}