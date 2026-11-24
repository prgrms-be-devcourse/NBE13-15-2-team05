package com.team5.auction.bid.dto;

import java.time.LocalDateTime;

import com.team5.auction.auction.domain.AuctionStatus;

/**
 * 내 입찰 목록 한 줄.
 *
 * @param highest 지금 내가 최고 입찰자인지
 */
public record MyBidResponse(
	Long bidId,
	Long auctionId,
	String auctionTitle,
	long amount,
	LocalDateTime bidAt,
	AuctionStatus auctionStatus,
	long currentPrice,
	boolean highest
) {
}