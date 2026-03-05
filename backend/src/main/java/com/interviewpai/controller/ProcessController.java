package com.interviewpai.controller;

import com.interviewpai.common.Result;
import com.interviewpai.entity.User;
import com.interviewpai.service.AudioProcessService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/process")
public class ProcessController {
    
    private final AudioProcessService audioProcessService;
    
    public ProcessController(AudioProcessService audioProcessService) {
        this.audioProcessService = audioProcessService;
    }
    
    @PostMapping("/audio/{audioId}")
    public Result<Void> processAudio(
            @AuthenticationPrincipal User user,
            @PathVariable Long audioId) {
        audioProcessService.processAudio(audioId);
        return Result.success();
    }
}
