package com.lujintao.coach.controller;

import com.lujintao.coach.common.ApiResult;
import com.lujintao.coach.model.Resume;
import com.lujintao.coach.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    public ApiResult<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        Resume resume = resumeService.upload(file);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resumeId", resume.getId());
        data.put("filename", resume.getFilename());
        data.put("summary", resume.getSummary());
        data.put("profile", resume.getProfileJson());
        return ApiResult.ok(data);
    }

    @GetMapping
    public ApiResult<List<Resume>> list() {
        return ApiResult.ok(resumeService.list());
    }
}
