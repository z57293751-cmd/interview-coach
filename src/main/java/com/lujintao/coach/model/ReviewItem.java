package com.lujintao.coach.model;

import lombok.Data;

@Data
public class ReviewItem {

    private Long id;
    private Long attemptId;
    private Long questionId;
    private Integer mastered;
    private String note;
    private String updatedAt;
}