package com.interviewpai.controller;

import com.interviewpai.common.Result;
import com.interviewpai.dto.QaRecordDTO;
import com.interviewpai.entity.Tag;
import com.interviewpai.entity.User;
import com.interviewpai.service.TagService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    
    private final TagService tagService;
    
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }
    
    @GetMapping
    public Result<List<Tag>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return Result.success(tags);
    }
    
    @GetMapping("/search")
    public Result<List<QaRecordDTO>> searchByKeyword(
            @AuthenticationPrincipal User user,
            @RequestParam String keyword) {
        List<QaRecordDTO> result = tagService.searchByKeyword(user.getId(), keyword);
        return Result.success(result);
    }
    
    @GetMapping("/search/{tag}")
    public Result<List<QaRecordDTO>> searchByTag(
            @AuthenticationPrincipal User user,
            @PathVariable String tag) {
        List<QaRecordDTO> result = tagService.searchByTag(user.getId(), tag);
        return Result.success(result);
    }
}
