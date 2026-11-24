package com.team5.auction.auction.event;

/** 경매 종료 스케줄러에서 FAILED 처리 시 발행. → AUCTION_FAILED: 판매자 */
public record AuctionFailedEvent(Long auctionId, String auctionTitle, Long sellerId) {
}
