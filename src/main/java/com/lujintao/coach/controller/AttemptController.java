package com.lujintao.coach.controller;

import com.lujintao.coach.common.ApiResult;
import com.lujintao.coach.model.Attempt;
import com.lujintao.coach.service.AttemptService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping
    public ApiResult<Map<String, Object>> submit(@RequestBody SubmitRequest request) {
        Attempt attempt = attemptService.submit(request.getQuestionId(), request.getUserAnswer());
        return ApiResult.ok(Map.of("attemptId", attempt.getId(), "status", attempt.getStatus()));
    }

    @PostMapping("/{id}/evaluate")
    public ApiResult<Attempt> evaluate(@PathVariable Long id) {
        return ApiResult.ok(attemptService.evaluate(id));
    }

    @GetMapping("/{id}")
    public ApiResult<Attempt> get(@PathVariable Long id) {
        return ApiResult.ok(attemptService.get(id));
    }

    @Data
    public static class SubmitRequest {
        private Long questionId;
        private String userAnswer;
    }
}