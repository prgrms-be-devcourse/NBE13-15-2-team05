package com.team5.auction.notification.domain;


public enum NotificationType {
	OUTBID,            // 상위 입찰 발생
	AUCTION_WON,       // 낙찰 (구매자)
	AUCTION_SOLD,      // 판매 성공 (판매자)
	AUCTION_FAILED,    // 유찰 (판매자)
	AUCTION_CANCELED,  // 운영자 경매 강제 취소 (판매자, 최고 입찰자)
	SHIPPED,           // 송장 입력 (구매자)
	CONFIRM_SOON,      // 자동 확정 하루 전 (구매자)
	TRADE_CANCELED     // 거래 취소 (구매자, 판매자)
}
