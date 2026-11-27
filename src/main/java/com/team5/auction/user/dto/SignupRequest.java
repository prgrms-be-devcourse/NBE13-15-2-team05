package com.team5.auction.user.dto;

import com.team5.auction.common.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식에 맞지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        String password,

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다.")
        String nickname,

        @NotBlank(message = "전화번호를 입력해주세요.")
        @Pattern(regexp = ValidationPatterns.PHONE, message = "전화번호는 010-1234-5678 형식이어야 합니다.")
        String phone
) {
}
