package com.team5.auction.trade.event;

import com.team5.auction.trade.domain.CancelReason;

/** 송장 기한 스케줄러·운영자 취소 시 발행. → TRADE_CANCELED: 구매자, 판매자 */
public record TradeCanceledEvent(Long tradeId, String auctionTitle, Long buyerId, Long sellerId, CancelReason reason) {
}
