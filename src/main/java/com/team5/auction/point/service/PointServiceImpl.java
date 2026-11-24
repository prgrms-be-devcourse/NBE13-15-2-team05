package com.team5.auction.point.service;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** TODO(포인트): 구현. 시그니처와 트랜잭션 설정은 계약이므로 바꾸지 않습니다. */
@Service
public class PointServiceImpl implements PointService {

	@Override
	@Transactional
	public void charge(Long userId, long amount) {
		throw new UnsupportedOperationException("TODO(포인트)");
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void lockForBid(Long auctionId, Long bidderId, long amount, @Nullable Long prevBidderId, long prevAmount) {
		throw new UnsupportedOperationException("TODO(포인트)");
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void unlockBid(Long auctionId, Long userId, long amount) {
		throw new UnsupportedOperationException("TODO(포인트)");
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void confirmPurchase(Long tradeId, Long buyerId, long price, Long sellerId, long settleAmount) {
		throw new UnsupportedOperationException("TODO(포인트)");
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void refund(Long tradeId, Long buyerId, long amount) {
		throw new UnsupportedOperationException("TODO(포인트)");
	}
}
