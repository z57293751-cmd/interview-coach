package com.lujintao.coach.model;

import lombok.Data;

@Data
public class Resume {

    private Long id;
    private String filename;
    private String contentText;
    private String profileJson;
    private String summary;
    private String createdAt;
}
