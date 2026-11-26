package com.team5.auction.user.controller;

import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.user.dto.MeResponse;
import com.team5.auction.user.dto.PatchMeRequest;
import com.team5.auction.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원 정보 조회")
    @GetMapping
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(userService.me(loginUser.id()));
    }

    @Operation(summary = "회원 정보 수정")
    @PatchMapping
    public ApiResponse<MeResponse> updateMe(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody PatchMeRequest request){
        return ApiResponse.ok(userService.updateMe(loginUser.id(), request));
    }
}
