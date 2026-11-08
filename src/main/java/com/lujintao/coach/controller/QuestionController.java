package com.lujintao.coach.controller;

import com.lujintao.coach.common.ApiResult;
import com.lujintao.coach.model.QuestionDetailVO;
import com.lujintao.coach.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ApiResult<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(questionService.page(keyword, category, page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<QuestionDetailVO> detail(@PathVariable Long id) {
        return ApiResult.ok(questionService.detail(id));
    }
}