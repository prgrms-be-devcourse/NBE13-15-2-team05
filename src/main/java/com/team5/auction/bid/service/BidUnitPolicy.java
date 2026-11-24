package com.team5.auction.bid.service;

/** 입찰 단위. 담당: 입찰 (계약서 3.1) */
public interface BidUnitPolicy {

	/** 현재가가 속한 구간의 최소 입찰 단위. maxPrice는 "이하" (R-13) */
	long unitFor(long currentPrice);
}
