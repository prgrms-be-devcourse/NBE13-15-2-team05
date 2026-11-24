package com.team5.auction.bid.dto;

import java.time.LocalDateTime;

/**
 * 경매의 입찰 내역 한 줄. 입찰자는 공개하지 않습니다(R-18).
 *
 * @param mine 로그인한 사용자 본인의 입찰인지. 비로그인이면 항상 false
 */
public record BidHistoryResponse(
	long amount,
	LocalDateTime bidAt,
	boolean mine
) {
}