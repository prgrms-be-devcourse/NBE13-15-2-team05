package com.team5.auction.trade.service;

import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.common.entity.Address;
import com.team5.auction.point.service.Fee;
import com.team5.auction.point.service.FeeCalculator;
import com.team5.auction.trade.domain.TradeRole;
import com.team5.auction.trade.domain.TradeStatus;
import com.team5.auction.trade.dto.TradeDetailResponse;
import com.team5.auction.trade.dto.TradeResponse;
import com.team5.auction.trade.event.TradeCreatedEvent;
import com.team5.auction.trade.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.user.domain.User;

import java.time.Clock;
import java.time.LocalDateTime;

/** TODO(거래): 구현 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeServiceImpl implements TradeService {

	private final FeeCalculator feeCalculator;
	private final Clock clock;
	private final TradeRepository tradeRepository;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final AuctionPolicyProperties policy;

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
		throw new UnsupportedOperationException("미구현");
	}

	@Override
	public TradeDetailResponse getTrade(Long userId, Long tradeId) {
		throw new UnsupportedOperationException("미구현");
	}

	@Override
	@Transactional
	public TradeDetailResponse registerTracking(Long userId, Long tradeId, String trackingNumber) {
		throw new UnsupportedOperationException("미구현");
	}

	@Override
	@Transactional
	public TradeDetailResponse confirmPurchase(Long userId, Long tradeId) {
		throw new UnsupportedOperationException("미구현");
	}

	@Override
	@Transactional
	public void cancelTradeById(Long userId, Long tradeId) {
		throw new UnsupportedOperationException("미구현");
	}
}
