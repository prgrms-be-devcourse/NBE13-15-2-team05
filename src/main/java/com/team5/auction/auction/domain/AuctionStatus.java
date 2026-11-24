package com.team5.auction.auction.domain;


public enum AuctionStatus {
	ACTIVE,      // 진행 중 (등록 즉시)
	SOLD,        // 마감, 입찰 있음
	BOUGHT_NOW,  // 즉시구매로 낙찰
	FAILED,      // 마감, 입찰 없음 (유찰)
	CANCELED     // 판매자(입찰 0건) 또는 운영자 취소
}
