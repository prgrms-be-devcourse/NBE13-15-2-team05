package com.team5.auction.trade.event;

/** 송장 입력 시 발행. → SHIPPED: 구매자 */
public record TradeShippedEvent(Long tradeId, String auctionTitle, Long buyerId, String trackingNumber) {
}
