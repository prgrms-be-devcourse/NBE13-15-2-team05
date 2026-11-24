package com.team5.auction.trade.event;

import com.team5.auction.trade.domain.TradeType;

/** createTrade 안에서 발행. → AUCTION_WON: 구매자, AUCTION_SOLD: 판매자 */
public record TradeCreatedEvent(Long tradeId, Long auctionId, String auctionTitle, Long buyerId, Long sellerId, TradeType type, long price) {
}
