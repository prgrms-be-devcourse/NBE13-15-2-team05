package com.team5.auction.bid.domain;

import java.time.LocalDateTime;

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
}
