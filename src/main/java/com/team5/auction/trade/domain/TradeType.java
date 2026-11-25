package com.team5.auction.trade.domain;

/** 거래 성립 방식 */
public enum TradeType {
	BID_WIN,  // 낙찰 (경매 종료 시 최고 입찰자)
	BUY_NOW   // 즉시구매
}
