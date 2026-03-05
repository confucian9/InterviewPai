package com.interviewpai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewSummaryDTO {
    private Long id;
    private Long audioId;
    private String summary;
    private String keywords;
    private LocalDateTime createTime;
}
