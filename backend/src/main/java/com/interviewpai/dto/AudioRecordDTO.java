package com.interviewpai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AudioRecordDTO {
    private Long id;
    private String fileName;
    private String fileUrl;
    private Long duration;
    private String status;
    private LocalDateTime createTime;
}
