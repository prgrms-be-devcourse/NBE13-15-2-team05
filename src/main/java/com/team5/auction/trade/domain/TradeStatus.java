package com.team5.auction.trade.domain;


public enum TradeStatus {
	PAID,       // 거래 성립, 구매자 포인트 에스크로 보관
	SHIPPED,    // 송장 입력
	CONFIRMED,  // 구매확정, 판매자 정산
	CANCELED    // 취소, 구매자 환불
}
