package com.lujintao.coach.model;

import lombok.Data;

@Data
public class Attempt {

    private Long id;
    private Long questionId;
    private String userAnswer;
    private Integer score;
    private String aiComment;
    private String missedPoints;
    private String followup;
    private String status;
    private String createdAt;
}