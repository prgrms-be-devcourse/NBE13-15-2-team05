package com.team5.auction.point.domain;


public enum BalanceHistoryType {
	CHARGE,      // 충전: available +
	BID_LOCK,    // 입찰 홀딩: available → locked
	BID_UNLOCK,  // 홀딩 해제: locked → available
	PURCHASE,    // 구매확정 차감: locked -
	REFUND,      // 거래 취소 환불: locked → available
	SETTLE       // 판매자 정산: available +
}
