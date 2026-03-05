package com.interviewpai.controller;

import com.interviewpai.common.PageResult;
import com.interviewpai.common.Result;
import com.interviewpai.dto.*;
import com.interviewpai.entity.User;
import com.interviewpai.service.InterviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {
    
    private final InterviewService interviewService;
    
    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }
    
    @GetMapping("/list")
    public Result<PageResult<AudioRecordDTO>> getInterviewList(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<AudioRecordDTO> result = interviewService.getInterviewList(user.getId(), page, size);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<InterviewDetailDTO> getInterviewDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        InterviewDetailDTO dto = interviewService.getInterviewDetail(user.getId(), id);
        return Result.success(dto);
    }
    
    @GetMapping("/{audioId}/qa")
    public Result<PageResult<QaRecordDTO>> getQaList(
            @AuthenticationPrincipal User user,
            @PathVariable Long audioId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<QaRecordDTO> result = interviewService.getQaList(user.getId(), audioId, page, size);
        return Result.success(result);
    }
    
    @PutMapping("/qa/{qaId}")
    public Result<Void> updateQa(
            @AuthenticationPrincipal User user,
            @PathVariable Long qaId,
            @RequestBody UpdateQaRequest request) {
        interviewService.updateQa(user.getId(), qaId, request);
        return Result.success();
    }
    
    @DeleteMapping("/qa/{qaId}")
    public Result<Void> deleteQa(
            @AuthenticationPrincipal User user,
            @PathVariable Long qaId) {
        interviewService.deleteQa(user.getId(), qaId);
        return Result.success();
    }
}
