package com.interviewpai.controller;

import com.interviewpai.common.Result;
import com.interviewpai.dto.*;
import com.interviewpai.entity.User;
import com.interviewpai.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }
    
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }
    
    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        UserDTO dto = userService.getCurrentUser(user.getId());
        return Result.success(dto);
    }
    
    @PutMapping("/me")
    public Result<Void> updateUser(@AuthenticationPrincipal User user, @RequestBody UpdateUserRequest request) {
        userService.updateUser(user.getId(), request);
        return Result.success();
    }
}
