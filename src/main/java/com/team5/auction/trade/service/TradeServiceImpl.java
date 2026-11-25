package com.team5.auction.trade.service;

import com.team5.auction.trade.domain.TradeRole;
import com.team5.auction.trade.domain.TradeStatus;
import com.team5.auction.trade.dto.TradeDetailResponse;
import com.team5.auction.trade.dto.TradeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.user.domain.User;

/** TODO(거래): 구현 */
@Service
@RequiredArgsConstructor
@Transactional
public class TradeServiceImpl implements TradeService {

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public Trade createTrade(Auction auction, User buyer, TradeType type, long price) {
		throw new UnsupportedOperationException("TODO(거래)");
	}

	@Override
	@Transactional(readOnly = true)
	public Page<TradeResponse> getTrades(Long userId, TradeRole role, TradeStatus status, Pageable pageable) {
		throw new UnsupportedOperationException("미구현");
	}

	@Override
	@Transactional(readOnly = true)
	public TradeDetailResponse getTrade(Long userId, Long tradeId) {
		throw new UnsupportedOperationException("미구현");
	}

	@Override
	public TradeDetailResponse registerTracking(Long userId, Long tradeId, String trackingNumber) {
		throw new UnsupportedOperationException("미구현");
	}

	@Override
	public TradeDetailResponse confirmPurchase(Long userId, Long tradeId) {
		throw new UnsupportedOperationException("미구현");
	}

	@Override
	public void cancelTradeById(Long userId, Long tradeId) {
		throw new UnsupportedOperationException("미구현");
	}

}
