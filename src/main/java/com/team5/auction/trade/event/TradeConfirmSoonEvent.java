package com.team5.auction.trade.event;

import java.time.LocalDateTime;

/** 확정 임박 스케줄러에서 발행 (confirmSoonNotified == false인 거래만, 발행 후 true). → CONFIRM_SOON: 구매자 */
public record TradeConfirmSoonEvent(Long tradeId, String auctionTitle, Long buyerId, LocalDateTime autoConfirmAt) {
}
