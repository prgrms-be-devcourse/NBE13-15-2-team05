package com.team5.auction.category.dto;
import jakarta.validation.constraints.Size ;
import jakarta.validation.constraints.NotBlank;

    public record CategoryRequest(
    @NotBlank(message = "카테고리 이름을 입력해주세요")
    @Size(max = 50, message = "카테고리 이름은 50자 이하여야 합니다")
    String name) {}


