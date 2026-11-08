package com.lujintao.coach.model;

import lombok.Data;

@Data
public class MockTurn {

    private Long id;
    private Long sessionId;
    private Integer seq;
    private String category;
    private String question;
    private String why;
    private String userAnswer;
    private Integer score;
    private String comment;
    private String missedPoints;
    private String followup;
    private String status;
    private String createdAt;
}
