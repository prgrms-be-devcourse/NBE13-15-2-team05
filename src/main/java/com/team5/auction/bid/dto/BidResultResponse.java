package com.team5.auction.bid.dto;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.bid.domain.Bid;

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

	/** 입찰 반영(applyBid, markBoughtNow)이 끝난 경매로 만든다 */
	public static BidResultResponse of(Bid bid, Auction auction, boolean extended, @Nullable Long tradeId) {
		return new BidResultResponse(
			bid.getId(),
			bid.getAmount(),
			auction.getCurrentPrice(),
			auction.getEndAt(),
			extended,
			auction.getStatus() == AuctionStatus.BOUGHT_NOW,
			tradeId);
	}
}