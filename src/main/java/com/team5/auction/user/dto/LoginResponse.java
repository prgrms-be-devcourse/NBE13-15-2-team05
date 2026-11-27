package com.team5.auction.user.dto;

public record LoginResponse(
        String accessToken,

        String tokenType,

        long expiresIn
) {
    public static LoginResponse of(String accessToken, long expiresIn){
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }
}
