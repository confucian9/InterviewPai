package com.interviewpai.dto;

import lombok.Data;

@Data
public class AudioUploadResponse {
    private Long audioId;
    private String fileUrl;
    private String fileName;
    
    public AudioUploadResponse(Long audioId, String fileUrl, String fileName) {
        this.audioId = audioId;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
    }
}
