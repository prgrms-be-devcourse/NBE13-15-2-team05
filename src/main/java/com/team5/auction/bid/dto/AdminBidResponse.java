package com.team5.auction.bid.dto;

import java.time.LocalDateTime;

/** 운영자용 입찰 내역. 실제 입찰자를 포함합니다(R-18). */
public record AdminBidResponse(
	Long bidId,
	long amount,
	LocalDateTime bidAt,
	Bidder bidder
) {

	public record Bidder(Long id, String nickname, String email) {
	}
}