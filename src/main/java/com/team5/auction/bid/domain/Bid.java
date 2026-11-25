package com.team5.auction.bid.domain;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.common.entity.BaseEntity;
import com.team5.auction.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 입찰. 즉시구매가 이상 입찰은 즉시구매가로 기록합니다 (R-43). 담당: 입찰 */
@Getter
@Entity
@Table(indexes = {
	@Index(name = "idx_bid_auction", columnList = "auction_id, id"),
	@Index(name = "idx_bid_bidder", columnList = "bidder_id, id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bid extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "auction_id", nullable = false)
	private Auction auction;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bidder_id", nullable = false)
	private User bidder;

	@Column(nullable = false)
	private long amount;

	@Column(nullable = false)
	private LocalDateTime bidAt;

	private Bid(Auction auction, User bidder, long amount, LocalDateTime bidAt) {
		this.auction = auction;
		this.bidder = bidder;
		this.amount = amount;
		this.bidAt = bidAt;
	}

	/** amount는 실제 처리 금액. 즉시구매가 이상 입찰이면 즉시구매가를 넘긴다 (R-43) */
	public static Bid create(Auction auction, User bidder, long amount, LocalDateTime bidAt) {
		return new Bid(auction, bidder, amount, bidAt);
	}

	/** 비로그인(null)이면 false. 지연 로딩이어도 getId()는 추가 조회가 없다 */
	public boolean isBidBy(@Nullable Long userId) {
		return userId != null && userId.equals(bidder.getId());
	}
}
