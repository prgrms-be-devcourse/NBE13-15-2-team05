package com.team5.auction.user.dto;

import com.team5.auction.user.domain.User;

public record SignupResponse(
        Long userId
) {
    public static SignupResponse from(User user){
        return new SignupResponse(user.getId());
    }
}
