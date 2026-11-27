package com.team5.auction.point.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.team5.auction.common.entity.RefType;
import com.team5.auction.point.domain.BalanceHistory;
import com.team5.auction.point.domain.BalanceHistoryType;
import com.team5.auction.point.repository.BalanceHistoryRepository;
import com.team5.auction.user.domain.User;

import lombok.RequiredArgsConstructor;

/** TODO(포인트): 구현. 시그니처와 트랜잭션 설정은 계약이므로 바꾸지 않습니다. */
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

	private final BalanceHistoryRepository balanceHistoryRepository;
	private final EntityManager entityManager;

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

	/** 유저들을 id 오름차순으로 하나씩 잠금. 잠그는 순서가 늘 같아서 데드락이 생기지 않도록 */
	private Map<Long, User> lockUsers(Long... userIds) {
		Map<Long, User> users = new HashMap<>();
		for (Long id : Arrays.stream(userIds).distinct().sorted().toList()) {
			users.put(id, lockUser(id));
		}
		return users;
	}

	/**
	 * 유저를 FOR UPDATE로 잠그면서 DB의 최신 잔액으로 다시 읽습니다.
	 * 호출한 쪽이 같은 트랜잭션에서 락 없이 먼저 읽은 유저는 1차 캐시에 옛 잔액이 남아 있기 때문입니다.
	 * 잠금 쿼리(findAllByIdInForUpdate)로 먼저 잠그면 Hibernate가 refresh를 락 없는 SELECT로 보내서
	 * MySQL(REPEATABLE READ)에서는 옛 잔액을 다시 읽습니다. 그래서 잠금과 다시 읽기를 refresh 한 번에 합니다.
	 * 이 트랜잭션에서 이미 잠근 유저는 다시 읽지 않습니다. 아직 DB에 반영하지 않은 변경이 덮어써지기 때문입니다.
	 */
	private User lockUser(Long id) {
		User user = entityManager.find(User.class, id);
		if (user == null) {
			throw new IllegalStateException("유저를 찾을 수 없습니다. id=" + id);
		}
		if (entityManager.getLockMode(user) != LockModeType.PESSIMISTIC_WRITE) {
			entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
		}
		return user;
	}

	/** 잔액을 바꾼 직후에 내역을 한 건 남깁니다 (R-22) */
	private void saveHistory(User user, BalanceHistoryType type, long amount, RefType refType, Long refId) {
		balanceHistoryRepository.save(BalanceHistory.create(user, type, amount, refType, refId));
	}
}
