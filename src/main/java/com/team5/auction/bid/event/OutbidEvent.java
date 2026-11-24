package com.team5.auction.bid.event;

/** 입찰 성공 시, 이전 최고 입찰자가 있을 때 발행 (즉시구매 포함). → OUTBID: 이전 최고 입찰자 */
public record OutbidEvent(Long auctionId, String auctionTitle, Long userId, long newPrice) {
}
