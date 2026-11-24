package com.team5.auction.trade.service;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.user.domain.User;

/** 거래. 담당: 거래 (계약서 5장) */
public interface TradeService {

	/**
	 * 거래를 만듭니다. 호출하는 쪽의 트랜잭션 안에서만 부릅니다 (MANDATORY).
	 * - 수수료: FeeCalculator로 계산해 스냅샷 (R-54)
	 * - 배송지: buyer.getAddress().copy()로 복사 (R-57)
	 * - status = PAID, tradedAt = now, shipDeadlineAt = now + shipDeadlineDays
	 * - TradeCreatedEvent 발행
	 * 포인트는 움직이지 않습니다. 구매자의 묶인 포인트가 그대로 에스크로 역할을 합니다. (R-42)
	 *
	 * 호출: 입찰 서비스(즉시구매, BUY_NOW, buyNowPrice) / 경매 종료 스케줄러(SOLD, BID_WIN, currentPrice)
	 */
	Trade createTrade(Auction auction, User buyer, TradeType type, long price);
}
