package com.team5.auction.point.service;

import org.jspecify.annotations.Nullable;

/**
 * 포인트 서비스. 유저 잔액은 이 서비스만 바꿉니다. (계약서 2장) 담당: 포인트
 * charge를 뺀 나머지는 호출하는 쪽의 트랜잭션 안에서만 부릅니다 (propagation = MANDATORY).
 * 유저 락은 이 서비스 안에서 id 오름차순으로 겁니다. 호출하는 쪽은 유저 락을 따로 걸지 않습니다.
 */
public interface PointService {

	/** 테스트용 충전. 자체 트랜잭션. CHARGE. (R-23) */
	void charge(Long userId, long amount);

	/**
	 * 입찰 성공 시 홀딩. 새 입찰자 available → locked (BID_LOCK).
	 * prevBidderId가 있으면 같은 호출 안에서 이전 최고 입찰자 locked → available (BID_UNLOCK).
	 * 사용 가능 포인트가 부족하면 BALANCE_INSUFFICIENT. (R-14, R-20, R-21)
	 */
	void lockForBid(Long auctionId, Long bidderId, long amount, @Nullable Long prevBidderId, long prevAmount);

	/** 운영자 경매 강제 취소 시 최고 입찰자 홀딩 해제. BID_UNLOCK. (R-61) */
	void unlockBid(Long auctionId, Long userId, long amount);

	/**
	 * 구매확정. 구매자 locked 차감(PURCHASE) + 판매자 available 증가(SETTLE).
	 * 수수료(price - settleAmount)는 플랫폼 수익입니다. (R-53)
	 */
	void confirmPurchase(Long tradeId, Long buyerId, long price, Long sellerId, long settleAmount);

	/** 거래 취소 환불. 구매자 locked → available (REFUND). (R-55, R-56) */
	void refund(Long tradeId, Long buyerId, long amount);
}
