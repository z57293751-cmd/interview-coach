package com.lujintao.coach.model;

import lombok.Data;

@Data
public class MockSession {

    private Long id;
    private Long resumeId;
    private String status;
    private Integer totalScore;
    private String createdAt;
    private String finishedAt;
}
