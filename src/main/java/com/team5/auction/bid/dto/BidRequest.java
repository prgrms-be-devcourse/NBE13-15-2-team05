package com.team5.auction.bid.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** POST /api/auctions/{id}/bids 요청. 최소 입찰가 검사(R-12, R-13)는 서비스에서 합니다. */
public record BidRequest(
	@NotNull @Positive Long amount
) {
}