package com.team5.auction.auction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

//수신 dto
public record AuctionCreateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 100) String title,
        @NotBlank String description,
        @NotNull @Positive Long startPrice,
        @Positive Long buyNowPrice,
        @NotNull @Positive int durationDays,
        @Size(max = 10) List<@NotBlank String> imageUrls) {
}