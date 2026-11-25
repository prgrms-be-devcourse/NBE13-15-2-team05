package com.team5.auction.auction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AuctionUpdateRequest(
        @NotNull Long categoryId,
        @NotNull@Size(max = 100) String title,
        String description,
        @NotNull @Positive Long startPrice,
        @Positive Long buyNowPrice,
        @NotNull @Positive int durationDays,
        @NotNull@Size(max = 10) List<@NotBlank String> imageUrls) {     //배열길이가 0일 때 @NotNull이 의미 있나?
}