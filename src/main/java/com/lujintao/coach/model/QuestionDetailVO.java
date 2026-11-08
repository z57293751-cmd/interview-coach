package com.lujintao.coach.model;

import lombok.Data;

@Data
public class QuestionDetailVO {

    private Long id;
    private String category;
    private String title;
    private String answerStandard;
    private String keyPoints;
    private Integer difficulty;
    private String source;
    private Integer lastScore;
    private Long lastAttemptId;
}