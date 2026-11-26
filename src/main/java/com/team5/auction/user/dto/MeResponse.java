package com.team5.auction.user.dto;

import com.team5.auction.user.domain.Role;
import com.team5.auction.user.domain.User;

public record MeResponse(
        Long id,
        String email,
        String nickname,
        String phone,
        Role role,
        long availableBalance,
        long lockedBalance,
        AddressDto address
) {
    public static MeResponse from(User user){
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPhone(),
                user.getRole(),
                user.getAvailableBalance(),
                user.getLockedBalance(),
                AddressDto.from(user.getAddress())
        );
    }
}
