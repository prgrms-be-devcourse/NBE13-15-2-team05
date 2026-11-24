package com.team5.auction.point.domain;

import com.team5.auction.common.entity.BaseEntity;
import com.team5.auction.common.entity.RefType;
import com.team5.auction.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 변동 내역. 잔액이 바뀔 때마다 한 건씩 남깁니다 (R-22). 담당: 포인트
 * refType: AUCTION(BID_LOCK, BID_UNLOCK), TRADE(PURCHASE, SETTLE, REFUND), null(CHARGE)
 */
@Getter
@Entity
@Table(indexes = @Index(name = "idx_balance_history_user", columnList = "user_id, id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BalanceHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BalanceHistoryType type;

	@Column(nullable = false)
	private long amount;

	@Column(nullable = false)
	private long availableAfter;

	@Column(nullable = false)
	private long lockedAfter;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private RefType refType;

	private Long refId;
}
