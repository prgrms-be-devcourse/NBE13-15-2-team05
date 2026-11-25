package com.team5.auction.bid.dto;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * 입찰·즉시구매 결과.
 *
 * @param extended  이번 입찰로 마감이 연장됐는지 (R-30)
 * @param boughtNow 즉시구매가에 도달해 낙찰됐는지 (R-40)
 * @param tradeId   낙찰됐을 때만 값이 있음
 */
public record BidResultResponse(
	Long bidId,
	long amount,
	long currentPrice,
	LocalDateTime endAt,
	boolean extended,
	boolean boughtNow,
	@Nullable Long tradeId
) {
}