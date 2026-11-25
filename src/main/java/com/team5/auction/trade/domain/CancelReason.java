package com.team5.auction.trade.domain;

/**
 * 거래 취소 사유. 어떤 사유든 취소되면 구매자에게 묶인 포인트를 환불합니다(REFUND).
 */
public enum CancelReason {
	/**
	 * 송장 기한 초과. 거래 성립 후 ship-deadline-days(3일) 안에 송장이 없으면
	 * 스케줄러가 자동 취소합니다. 판매자에게 불이익은 없습니다. (R-56, R-58)
	 */
	SHIP_TIMEOUT,
	/** 운영자 취소. 구매확정 전(PAID, SHIPPED)에만 할 수 있습니다. (R-55) */
	ADMIN
}
