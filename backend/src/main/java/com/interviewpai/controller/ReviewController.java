package com.interviewpai.controller;

import com.interviewpai.common.Result;
import com.interviewpai.dto.QaRecordDTO;
import com.interviewpai.entity.User;
import com.interviewpai.service.ReviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    
    private final ReviewService reviewService;
    
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }
    
    @GetMapping("/random")
    public Result<List<QaRecordDTO>> getRandomQuestions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") Integer count) {
        List<QaRecordDTO> questions = reviewService.getRandomQuestions(user.getId(), count);
        return Result.success(questions);
    }
    
    @GetMapping("/tag/{tag}")
    public Result<List<QaRecordDTO>> getQuestionsByTag(
            @AuthenticationPrincipal User user,
            @PathVariable String tag,
            @RequestParam(defaultValue = "10") Integer count) {
        List<QaRecordDTO> questions = reviewService.getQuestionsByTag(user.getId(), tag, count);
        return Result.success(questions);
    }
    
    @GetMapping("/all")
    public Result<List<QaRecordDTO>> getAllQuestionsForReview(
            @AuthenticationPrincipal User user) {
        List<QaRecordDTO> questions = reviewService.getAllQuestionsForReview(user.getId());
        return Result.success(questions);
    }
}
