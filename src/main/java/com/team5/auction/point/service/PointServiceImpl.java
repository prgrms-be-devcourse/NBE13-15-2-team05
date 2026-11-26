package com.team5.auction.point.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.team5.auction.common.entity.RefType;
import com.team5.auction.point.domain.BalanceHistory;
import com.team5.auction.point.domain.BalanceHistoryType;
import com.team5.auction.point.repository.BalanceHistoryRepository;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** TODO(포인트): 구현. 시그니처와 트랜잭션 설정은 계약이므로 바꾸지 않습니다. */
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

	private final UserRepository userRepository;
	private final BalanceHistoryRepository balanceHistoryRepository;

	@Override
	@Transactional
	public void charge(Long userId, long amount) {
		throw new UnsupportedOperationException("TODO(포인트)");
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void lockForBid(Long auctionId, Long bidderId, long amount, @Nullable Long prevBidderId, long prevAmount) {
		Map<Long, User> users = prevBidderId == null ? lockUsers(bidderId) : lockUsers(bidderId, prevBidderId);

		// 새 입찰자 홀딩을 먼저 한다. 포인트가 모자라면 여기서 멈춰서 아무 잔액도 바뀌지 않는다 (R-14, R-20)
		User bidder = users.get(bidderId);
		bidder.lock(amount);
		saveHistory(bidder, BalanceHistoryType.BID_LOCK, amount, RefType.AUCTION, auctionId);

		// 이전 최고 입찰자 해제 (R-21)
		if (prevBidderId != null) {
			User prevBidder = users.get(prevBidderId);
			prevBidder.unlock(prevAmount);
			saveHistory(prevBidder, BalanceHistoryType.BID_UNLOCK, prevAmount, RefType.AUCTION, auctionId);
		}
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

	/** 유저 행에 쓰기 락을 겁니다. 쿼리가 id 오름차순으로 잠가서 데드락이 생기지 않습니다 (계약서 2.2) */
	private Map<Long, User> lockUsers(Long... userIds) {
		List<Long> ids = Arrays.stream(userIds).distinct().toList();
		Map<Long, User> users = userRepository.findAllByIdInForUpdate(ids).stream()
				.collect(Collectors.toMap(User::getId, user -> user));
		if (users.size() != ids.size()) {
			throw new IllegalStateException("유저를 찾을 수 없습니다. ids=" + ids);
		}
		return users;
	}

	/** 잔액을 바꾼 직후에 내역을 한 건 남깁니다 (R-22) */
	private void saveHistory(User user, BalanceHistoryType type, long amount, RefType refType, Long refId) {
		balanceHistoryRepository.save(BalanceHistory.create(user, type, amount, refType, refId));
	}
}
