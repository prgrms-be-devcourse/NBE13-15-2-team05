package com.team5.auction.bid.dto;

import java.time.LocalDateTime;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.bid.event.BidPlacedEvent;

/**
 * 실시간 현재가 (SSE price 이벤트의 data). 최고 입찰자는 넣지 않는다 (R-18).
 *
 * @param extended 이번 입찰로 마감이 연장됐는지. 처음 연결할 때 보내는 현재 상태에서는 항상 false
 * @param status   ACTIVE가 아니면(즉시구매 등) 화면에서 입찰을 막고 연결을 닫으면 된다
 */
public record AuctionLiveResponse(
	Long auctionId,
	long currentPrice,
	long minimumNextBid,
	int bidCount,
	LocalDateTime endAt,
	boolean extended,
	AuctionStatus status
) {

	/** 처음 연결할 때 보내는 현재 상태 */
	public static AuctionLiveResponse of(Auction auction, long minimumNextBid) {
		return new AuctionLiveResponse(auction.getId(), auction.getCurrentPrice(), minimumNextBid,
			auction.getBidCount(), auction.getEndAt(), false, auction.getStatus());
	}

	public static AuctionLiveResponse from(BidPlacedEvent event) {
		return new AuctionLiveResponse(event.auctionId(), event.currentPrice(), event.minimumNextBid(),
			event.bidCount(), event.endAt(), event.extended(), event.status());
	}
}
