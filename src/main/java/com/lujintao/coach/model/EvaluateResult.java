package com.lujintao.coach.model;

import lombok.Data;

@Data
public class EvaluateResult {

    private Integer score;
    private String missedPoints;
    private String followup;
    private String comment;
    private boolean parsed;
}