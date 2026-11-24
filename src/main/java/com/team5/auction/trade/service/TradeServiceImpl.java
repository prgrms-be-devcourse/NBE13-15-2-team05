package com.team5.auction.trade.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.user.domain.User;

/** TODO(거래): 구현 */
@Service
public class TradeServiceImpl implements TradeService {

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public Trade createTrade(Auction auction, User buyer, TradeType type, long price) {
		throw new UnsupportedOperationException("TODO(거래)");
	}
}
