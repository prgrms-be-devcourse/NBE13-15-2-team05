package com.team5.auction.bid.dto;

import java.time.LocalDateTime;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.bid.domain.Bid;

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

	/** 경매를 함께 불러온 Bid로 만든다 (findWithAuctionByBidderId) */
	public static MyBidResponse of(Bid bid, Long bidderId) {
		Auction auction = bid.getAuction();
		return new MyBidResponse(
			bid.getId(),
			auction.getId(),
			auction.getTitle(),
			bid.getAmount(),
			bid.getBidAt(),
			auction.getStatus(),
			auction.getCurrentPrice(),
			auction.isHighestBidder(bidderId));
	}
}