package com.lujintao.coach.controller;

import com.lujintao.coach.common.ApiResult;
import com.lujintao.coach.service.MockInterviewService;
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
@RequestMapping("/api/mock")
@RequiredArgsConstructor
public class MockController {

    private final MockInterviewService mockInterviewService;

    /** 开始一场模拟面试，返回第 1 题 */
    @PostMapping("/sessions")
    public ApiResult<Map<String, Object>> start(@RequestBody StartRequest request) {
        return ApiResult.ok(mockInterviewService.start(request.getResumeId()));
    }

    /** 回答当前这题，返回评分 + 追问 + 下一题 */
    @PostMapping("/sessions/{id}/answer")
    public ApiResult<Map<String, Object>> answer(@PathVariable Long id, @RequestBody AnswerRequest request) {
        return ApiResult.ok(mockInterviewService.answer(id, request.getAnswer()));
    }

    /** 面试报告 */
    @GetMapping("/sessions/{id}")
    public ApiResult<Map<String, Object>> report(@PathVariable Long id) {
        return ApiResult.ok(mockInterviewService.report(id));
    }

    @Data
    public static class StartRequest {
        private Long resumeId;
    }

    @Data
    public static class AnswerRequest {
        private String answer;
    }
}
