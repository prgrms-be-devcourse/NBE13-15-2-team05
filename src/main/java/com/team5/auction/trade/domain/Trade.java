package com.team5.auction.trade.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.common.entity.Address;
import com.team5.auction.common.entity.BaseEntity;
import com.team5.auction.point.service.Fee;
import com.team5.auction.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 거래. 경매가 SOLD, BOUGHT_NOW일 때만 생성합니다. 담당: 거래 */
@Getter
@Entity
@Table(indexes = {
	@Index(name = "idx_trade_status_ship_deadline", columnList = "status, shipDeadlineAt"),
	@Index(name = "idx_trade_status_auto_confirm", columnList = "status, autoConfirmAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "auction_id", nullable = false, unique = true)
	private Auction auction;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "buyer_id", nullable = false)
	private User buyer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TradeType type;

	@Column(nullable = false)
	private long price;

	// ---- 수수료 스냅샷 (R-54) ----
	@Column(nullable = false, precision = 5, scale = 4)
	private BigDecimal feeRate;

	@Column(nullable = false)
	private long feeAmount;

	@Column(nullable = false)
	private long settleAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TradeStatus status;

	/** 거래 성립 시 구매자 기본 배송지 복사본 (R-57) */
	@Embedded
	private Address shippingAddress;

	@Column(length = 50)
	private String trackingNumber;

	@Column(nullable = false)
	private LocalDateTime tradedAt;

	/** tradedAt + ship-deadline-days (R-56) */
	@Column(nullable = false)
	private LocalDateTime shipDeadlineAt;

	private LocalDateTime shippedAt;

	/** shippedAt + auto-confirm-days (R-52) */
	private LocalDateTime autoConfirmAt;

	/** 자동 확정 임박 알림을 보냈는지. 중복 발송 방지 */
	@Column(nullable = false)
	private boolean confirmSoonNotified;

	private LocalDateTime confirmedAt;

	private LocalDateTime canceledAt;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private CancelReason cancelReason;

	public static Trade create(Auction auction, User buyer, TradeType type, long price, Fee fee, Address shippingAddress,
							   LocalDateTime now, int shipDeadlineDays) {
		Trade trade = new Trade();
		trade.auction = auction;
		trade.buyer = buyer;
		trade.seller = auction.getSeller();
		trade.type = type;
		trade.price = price;
		trade.feeRate = fee.rate();
		trade.feeAmount = fee.feeAmount();
		trade.settleAmount = fee.settleAmount();
		trade.status = TradeStatus.PAID;
		trade.shippingAddress = shippingAddress;
		trade.tradedAt = now;
		trade.shipDeadlineAt = now.plusDays(shipDeadlineDays);
		return trade;
	}
}
