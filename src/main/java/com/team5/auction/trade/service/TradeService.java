package com.team5.auction.trade.service;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeRole;
import com.team5.auction.trade.domain.TradeStatus;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.trade.dto.TradeDetailResponse;
import com.team5.auction.trade.dto.TradeResponse;
import com.team5.auction.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

	/**
	 * 내 거래 목록을 조회합니다.
	 * role이 BUYER면 내가 구매자인 거래, SELLER면 내가 판매자인 거래입니다.
	 * status가 null이면 전체 상태를 조회합니다.
	 */
	Page<TradeResponse> getTrades(Long userId, TradeRole role, TradeStatus status, Pageable pageable);

	/**
	 * 거래 상세를 조회합니다. 당사자만 가능합니다.
	 * - 상대방 연락처 포함 (R-50)
	 * - 수수료, 정산금은 판매자에게만 내려줌
	 * 에러: TRADE_NOT_FOUND, TRADE_NOT_PARTICIPANT
	 */
	TradeDetailResponse getTrade(Long userId, Long tradeId);

	/**
	 * 송장번호를 등록합니다. 판매자만 가능합니다. (R-51)
	 * - status: PAID → SHIPPED, shippedAt = now
	 * - autoConfirmAt = now + autoConfirmDays (R-52)
	 * 에러: TRADE_NOT_PARTICIPANT, TRADE_INVALID_STATUS(PAID가 아님)
	 */
	TradeDetailResponse registerTracking(Long userId, Long tradeId, String trackingNumber);

	/**
	 * 구매를 확정합니다. 구매자만 가능합니다. (R-52)
	 * - status: SHIPPED → CONFIRMED, confirmedAt = now
	 * - 구매자 포인트 차감, 판매자 정산 (R-53)
	 * 에러: TRADE_NOT_PARTICIPANT, TRADE_INVALID_STATUS(SHIPPED가 아님)
	 */
	TradeDetailResponse confirmPurchase(Long userId, Long tradeId);

	/**
	 * 운영자가 거래를 취소하고 구매자에게 환불합니다. (R-55)
	 * 에러: TRADE_NOT_FOUND, TRADE_INVALID_STATUS(CONFIRMED, CANCELED)
	 */
	void cancelTradeById(Long userId, Long tradeId);
}
