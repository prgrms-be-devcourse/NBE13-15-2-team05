package com.team5.auction.bid.event;

import java.time.LocalDateTime;

import com.team5.auction.auction.domain.AuctionStatus;

/**
 * 입찰 성공 시 발행 (즉시구매 포함). → 경매를 보고 있는 화면에 실시간 현재가 전송 (SSE)
 * 입찰 도메인 안에서만 쓰는 이벤트라 계약서의 이벤트 7종에는 들어가지 않는다.
 * 최고 입찰자는 공개하지 않으므로 넣지 않는다 (R-18).
 */
public record BidPlacedEvent(
	Long auctionId,
	long currentPrice,
	long minimumNextBid,
	int bidCount,
	LocalDateTime endAt,
	boolean extended,
	AuctionStatus status
) {
}
