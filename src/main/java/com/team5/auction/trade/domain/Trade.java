package com.team5.auction.trade.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.common.entity.Address;
import com.team5.auction.common.entity.BaseEntity;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.point.service.Fee;
import com.team5.auction.trade.exception.TradeErrorCode;
import com.team5.auction.user.domain.User;

import jakarta.persistence.*;
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

	@Version
	private Long version;

	/** 거래 생성. PAID 상태, 수수료·배송지 스냅샷 (R-42, R-54, R-57) */
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

	/** 구매자 여부 */
	public boolean isBuyer(Long userId) {
		return buyer.getId().equals(userId);
	}

	/** 판매자 여부 */
	public boolean isSeller(Long userId) {
		return seller.getId().equals(userId);
	}

	/** 거래 당사자(구매자·판매자) 여부 */
	public boolean isParticipant(Long userId) {
		return isBuyer(userId) || isSeller(userId);
	}

	/** 송장 등록. PAID → SHIPPED (R-51, R-52) */
	public void registerTracking(String trackingNumber, LocalDateTime now, int autoConfirmDays) {
		if (this.status != TradeStatus.PAID) {
			throw new BusinessException(TradeErrorCode.TRADE_INVALID_STATUS);
		}
		this.trackingNumber = trackingNumber;
		this.status = TradeStatus.SHIPPED;
		this.shippedAt = now;
		this.autoConfirmAt = now.plusDays(autoConfirmDays);
	}

	/** 구매확정. SHIPPED → CONFIRMED (R-52) */
	public void confirm(LocalDateTime now) {
		if (this.status != TradeStatus.SHIPPED) {
			throw new BusinessException(TradeErrorCode.TRADE_INVALID_STATUS);
		}
		this.status = TradeStatus.CONFIRMED;
		this.confirmedAt = now;
	}

	/** 거래 취소. PAID·SHIPPED → CANCELED (R-55, R-56) */
	public void cancel(CancelReason reason, LocalDateTime now) {
		if (this.status != TradeStatus.PAID && this.status != TradeStatus.SHIPPED) {
			throw new BusinessException(TradeErrorCode.TRADE_INVALID_STATUS);
		}
		this.status = TradeStatus.CANCELED;
		this.cancelReason = reason;
		this.canceledAt = now;
	}
}
