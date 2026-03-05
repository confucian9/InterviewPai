package com.interviewpai.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateQaRequest {
    private String question;
    private String answer;
    private List<String> tags;
}
