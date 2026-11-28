package com.team5.auction.auction.dto;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

import com.team5.auction.auction.domain.AuctionStatus;

public record AuctionListResponse(
	Long id,
	String title,
	String thumbnailUrl,
	String categoryName,
	long currentPrice,
	@Nullable Long buyNowPrice,
	int bidCount,
	AuctionStatus status,
	LocalDateTime endAt
) {
}
