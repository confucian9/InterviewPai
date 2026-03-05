package com.interviewpai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QaRecordDTO {
    private Long id;
    private Long audioId;
    private String question;
    private String answer;
    private String tags;
    private BigDecimal confidence;
    private LocalDateTime createTime;
}
