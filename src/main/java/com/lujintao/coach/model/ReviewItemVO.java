package com.lujintao.coach.model;

import lombok.Data;

@Data
public class ReviewItemVO {

    private Long id;
    private Long attemptId;
    private Long questionId;
    private String title;
    private String category;
    private Integer score;
    private String userAnswer;
    private String aiComment;
    private Integer mastered;
    private String note;
    private String updatedAt;
}