package com.interviewpai.controller;

import com.interviewpai.common.PageResult;
import com.interviewpai.common.Result;
import com.interviewpai.dto.AudioRecordDTO;
import com.interviewpai.dto.AudioUploadResponse;
import com.interviewpai.entity.User;
import com.interviewpai.service.AudioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audio")
public class AudioController {
    
    private final AudioService audioService;
    
    public AudioController(AudioService audioService) {
        this.audioService = audioService;
    }
    
    @PostMapping("/upload")
    public Result<AudioUploadResponse> uploadAudio(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        AudioUploadResponse response = audioService.uploadAudio(user.getId(), file);
        return Result.success(response);
    }
    
    @GetMapping("/list")
    public Result<PageResult<AudioRecordDTO>> getAudioList(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<AudioRecordDTO> result = audioService.getAudioList(user.getId(), page, size);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<AudioRecordDTO> getAudioDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        AudioRecordDTO dto = audioService.getAudioDetail(user.getId(), id);
        return Result.success(dto);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteAudio(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        audioService.deleteAudio(user.getId(), id);
        return Result.success();
    }
}
