package com.team5.auction.trade.domain;


public enum CancelReason {
	SHIP_TIMEOUT,  // 송장 기한 초과 (R-56)
	ADMIN          // 운영자 취소 (R-55)
}
