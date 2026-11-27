package com.team5.auction.trade.service;

import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.common.entity.Address;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.point.service.Fee;
import com.team5.auction.point.service.FeeCalculator;
import com.team5.auction.point.service.PointService;
import com.team5.auction.trade.domain.*;
import com.team5.auction.trade.dto.TradeDetailResponse;
import com.team5.auction.trade.dto.TradeResponse;
import com.team5.auction.trade.event.TradeCanceledEvent;
import com.team5.auction.trade.event.TradeCreatedEvent;
import com.team5.auction.trade.event.TradeShippedEvent;
import com.team5.auction.trade.exception.TradeErrorCode;
import com.team5.auction.trade.repository.TradeRepository;
import com.team5.auction.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.user.domain.User;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

// TODO(거래): 송장 기한(shipDeadlineAt) 초과 시 자동 취소, 환불 스케줄러 구현 (R-56)
// TODO(거래): autoConfirmAt 경과 시 자동 구매확정 스케줄러 구현 (R-52)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeServiceImpl implements TradeService {

	private final FeeCalculator feeCalculator;
	private final Clock clock;
	private final TradeRepository tradeRepository;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final AuctionPolicyProperties policy;
	private final PointService pointService;

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public Trade createTrade(Auction auction, User buyer, TradeType type, long price) {
		// 입찰 시 배송지를 확인하므로(R-19, R-66) 여기서 없으면 코드 버그
		if (!buyer.hasAddress()) {
			throw new IllegalStateException("구매자 배송지가 없습니다. buyerId= " + buyer.getId());
		}

		Fee fee = feeCalculator.calculate(price); // 수수료 스냅샷 (R-54)
		Address shippingAddress = buyer.getAddress().copy(); // 배송지 복사 (R-57)
		LocalDateTime now = LocalDateTime.now(clock);

		// PAID 상태로 생성, 송장 기한 = now + shipDeadlineDays (R-56)
		Trade trade = tradeRepository.save(Trade.create(auction, buyer, type, price, fee, shippingAddress,
				now, policy.trade().shipDeadlineDays()));

		// 알림 담당이 구독 → 구매자 AUCTION_WON, 판매자 AUCTION_SOLD
		applicationEventPublisher.publishEvent(new TradeCreatedEvent(
				trade.getId(), auction.getId(), auction.getTitle(),
				buyer.getId(), trade.getSeller().getId(), type, price
		));

		return trade;
	}

	@Override
	public Page<TradeResponse> getTrades(Long userId, TradeRole role, TradeStatus status, Pageable pageable) {
		// 역할에 따라 내가 구매자/판매자인 거래만 조회, status가 없으면 전체 상태
		Page<Trade> trades = switch (role) {
			case BUYER -> status == null
					? tradeRepository.findByBuyerId(userId, pageable)
					: tradeRepository.findByBuyerIdAndStatus(userId, status, pageable);
			case SELLER -> status == null
					? tradeRepository.findBySellerId(userId, pageable)
					: tradeRepository.findBySellerIdAndStatus(userId, status, pageable);
		};

		return trades.map(TradeResponse::from);
	}

	@Override
	public TradeDetailResponse getTrade(Long userId, Long tradeId) {
		// 거래 조회
		Trade trade = findTrade(tradeId);

		// 구매자·판매자만 조회 가능
		validateParticipant(trade, userId);

		// 상대방 연락처 포함(R-50), 수수료·정산금은 판매자에게만
		return TradeDetailResponse.of(trade, trade.isSeller(userId));
	}

	@Override
	@Transactional
	public TradeDetailResponse registerTracking(Long userId, Long tradeId, String trackingNumber) {
		Trade trade = findTrade(tradeId);

		// 판매자만 입력 가능 (R-51)
		if (!trade.isSeller(userId)) {
			throw new BusinessException(TradeErrorCode.TRADE_NOT_PARTICIPANT);
		}

		// PAID → SHIPPED, 자동 구매확정 시각 설정 (R-52)
		trade.registerTracking(trackingNumber, LocalDateTime.now(clock), policy.trade().autoConfirmDays());

		// 구매자에게 배송 알림
		applicationEventPublisher.publishEvent(new TradeShippedEvent(tradeId, trade.getAuction().getTitle(),
				trade.getBuyer().getId(),trackingNumber));

		// 판매자 기준 응답
		return TradeDetailResponse.of(trade, true);
	}

	@Override
	@Transactional
	public TradeDetailResponse confirmTrade(Long userId, Long tradeId) {
		Trade trade = findTrade(tradeId);

		// 구매자만 확정 가능 (R-52)
		if (!trade.isBuyer(userId)) {
			throw new BusinessException(TradeErrorCode.TRADE_NOT_PARTICIPANT);
		}

		confirm(trade);

		// 구매자 기준 응답
		return TradeDetailResponse.of(trade, false);
	}

	/** 구매확정 공통 처리. 구매자 확정과 자동 확정 스케줄러가 같이 씁니다. (R-52, R-53) */
	private void confirm(Trade trade) {
		// SHIPPED → CONFIRMED
		trade.confirm(LocalDateTime.now(clock));

		// 구매자 포인트 차감, 판매자 정산 (R-53)
		pointService.confirmPurchase(trade.getId(), trade.getBuyer().getId(), trade.getPrice(),
				trade.getSeller().getId(),trade.getSettleAmount());
	}

	@Override
	@Transactional
	public void cancelTradeById(Long userId, Long tradeId) {
		Trade trade = findTrade(tradeId);

		// 운영자 취소, 확정 전만 가능 (R-55)
		cancel(trade, CancelReason.ADMIN);
	}

	/** 거래 취소 공통 처리. 운영자 취소와 송장 기한 스케줄러가 같이 씁니다. (R-55, R-56) */
	private void cancel(Trade trade, CancelReason reason) {
		// PAID·SHIPPED → CANCELED
		trade.cancel(reason, LocalDateTime.now(clock));

		// 구매자에게 거래 금액 전액 환불
		pointService.refund(trade.getId(), trade.getBuyer().getId(), trade.getPrice());

		// 구매자·판매자에게 취소 알림
		applicationEventPublisher.publishEvent(new TradeCanceledEvent(trade.getId(), trade.getAuction().getTitle(),
				trade.getBuyer().getId(), trade.getSeller().getId(), reason));
	}

	/** 거래 조회. 없으면 TRADE_NOT_FOUND */
	private Trade findTrade (Long tradeId) {
		return tradeRepository.findById(tradeId)
				.orElseThrow(() -> new BusinessException(TradeErrorCode.TRADE_NOT_FOUND));
	}

	/** 구매자·판매자가 아니면 TRADE_NOT_PARTICIPANT */
	private void validateParticipant(Trade trade, Long userId) {
		if (!trade.isParticipant(userId)) {
			throw new BusinessException(TradeErrorCode.TRADE_NOT_PARTICIPANT);
		}
	}
}
