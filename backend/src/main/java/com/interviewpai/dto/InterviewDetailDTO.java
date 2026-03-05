package com.interviewpai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterviewDetailDTO {
    private Long id;
    private String fileName;
    private String fileUrl;
    private Long duration;
    private String status;
    private LocalDateTime createTime;
    private String transcript;
    private List<QaRecordDTO> qaList;
    private InterviewSummaryDTO summary;
}
