package com.lujintao.coach.model;

import lombok.Data;

@Data
public class QuestionItemVO {

    private Long id;
    private String category;
    private String title;
    private Integer difficulty;
    private Integer lastScore;
}