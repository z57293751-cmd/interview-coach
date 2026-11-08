package com.lujintao.coach.controller;

import com.lujintao.coach.common.ApiResult;
import com.lujintao.coach.service.ReviewService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResult<Map<String, Object>> list(@RequestParam(defaultValue = "0") Integer mastered) {
        return ApiResult.ok(reviewService.list(mastered));
    }

    @PatchMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody UpdateRequest request) {
        reviewService.update(id, request.getMastered(), request.getNote());
        return ApiResult.ok();
    }

    @Data
    public static class UpdateRequest {
        private Integer mastered;
        private String note;
    }
}