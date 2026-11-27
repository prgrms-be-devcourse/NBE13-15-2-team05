package com.team5.auction.user.controller;

import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.user.dto.LoginRequest;
import com.team5.auction.user.dto.LoginResponse;
import com.team5.auction.user.dto.SignupRequest;
import com.team5.auction.user.dto.SignupResponse;
import com.team5.auction.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request){
        return ApiResponse.ok(authService.signup(request));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        return ApiResponse.ok(authService.login(request));
    }
}
