package com.team5.auction.user.dto;

import jakarta.validation.constraints.Size;

public record PatchMeRequest(
        @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다.")
        String nickname,

        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phone,

        AddressDto address,
        Boolean clearAddress
) {
        /** 보내지 않았으면(null) false로 봅니다. */
        public boolean shouldClearAddress() {
                return Boolean.TRUE.equals(clearAddress);
        }
}
