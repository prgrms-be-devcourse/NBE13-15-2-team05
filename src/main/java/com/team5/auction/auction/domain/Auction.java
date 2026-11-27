package com.team5.auction.auction.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.team5.auction.category.domain.Category;
import com.team5.auction.common.entity.BaseEntity;
import com.team5.auction.user.domain.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경매. 담당: 경매 (입찰 관련 메서드는 입찰 담당이 구현, 계약서 4장)
 */
@Getter
@Entity
@Table(indexes = @Index(name = "idx_auction_status_end_at", columnList = "status, endAt"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(nullable = false, length = 100)
	private String title;

	@Lob
	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private long startPrice;

	/** 즉시구매가. null이면 즉시구매 불가. (R-03) */
	private Long buyNowPrice;

	@Column(nullable = false)
	private long currentPrice;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "highest_bidder_id")
	private User highestBidder;

	@Column(nullable = false)
	private int bidCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuctionStatus status;

	@Column(nullable = false)
	private LocalDateTime startAt;

	@Column(nullable = false)
	private LocalDateTime endAt;

	@Column(nullable = false)
	private LocalDateTime originalEndAt;

	// ---- 마감 연장 설정 스냅샷 (등록 시점 값을 복사, R-05) ----
	@Column(nullable = false)
	private boolean extendEnabled;

	@Column(nullable = false)
	private int extendTriggerMinutes;

	@Column(nullable = false)
	private int extendMinutes;

	@Column(nullable = false)
	private int extendCount;

	/** 동시 입찰 제어 (R-16) */
	@Version
	private Long version;

	@OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<AuctionImage> images = new ArrayList<>();

	// ================= 입찰 담당 구현 (계약서 4장) =================

	/** status == ACTIVE && now < endAt (R-17) */
	public boolean isOpenAt(LocalDateTime now) {
		return status == AuctionStatus.ACTIVE && now.isBefore(endAt);
	}

	/** 입찰 0건이면 startPrice, 아니면 currentPrice + bidUnit (R-12) */
	public long minimumBid(long bidUnit) {
		return bidCount == 0 ? startPrice : currentPrice + bidUnit;
	}

	/** buyNowPrice != null && currentPrice < buyNowPrice (R-03, R-45) */
	public boolean canBuyNow() {
		return buyNowPrice != null && currentPrice < buyNowPrice;
	}

	/** 즉시구매가 이상 입찰인지. 맞으면 입찰 서비스가 즉시구매가로 바꿔 처리한다 (R-43) */
	public boolean reachesBuyNow(long amount) {
		return canBuyNow() && amount >= buyNowPrice;
	}

	/**
	 * 현재가·최고 입찰자 갱신, 입찰 수 +1, 마감 연장 판단 (R-30, 계약서 4장 계산식)
	 *
	 * @return 이번 입찰로 마감이 연장됐는지
	 */
	public boolean applyBid(User bidder, long amount, LocalDateTime now) {
		currentPrice = amount;
		highestBidder = bidder;
		bidCount++;

		// 마감 extendTriggerMinutes 전부터(경계 포함) 입찰이 들어오면 now + extendMinutes로 연장.
		// 연장 결과가 기존 마감보다 늦을 때만 반영해 마감이 앞당겨지지 않게 한다. originalEndAt은 유지, 횟수 상한 없음 (R-31)
		if (extendEnabled && !now.isBefore(endAt.minusMinutes(extendTriggerMinutes))) {
			LocalDateTime extended = now.plusMinutes(extendMinutes);
			if (extended.isAfter(endAt)) {
				endAt = extended;
				extendCount++;
				return true;
			}
		}
		return false;
	}

	/** ACTIVE → BOUGHT_NOW */
	public void markBoughtNow() {
		if (status != AuctionStatus.ACTIVE)
			throw new IllegalStateException("ACTIVE 경매만 즉시구매 가능");
		status = AuctionStatus.BOUGHT_NOW;
	}

	/** 비로그인(null)이면 false */
	public boolean isHighestBidder(Long userId) {
		return highestBidder != null && userId != null && userId.equals(highestBidder.getId());
	}

	/** 판매자 본인인지 (R-10) */
	public boolean isSeller(Long userId) {
		return userId != null && userId.equals(seller.getId());
	}

	// ================= 경매 담당 구현 =================

	/** 스케줄러에서 호출. 입찰이 있으면 SOLD, 없으면 FAILED (R-41) */
	public void close() {
		throw new UnsupportedOperationException("TODO(경매)");
	}

	/** ACTIVE → CANCELED (판매자 또는 운영자) */
	public void cancel() {
		throw new UnsupportedOperationException("TODO(경매)");
	}

	/** status == ACTIVE && bidCount == 0 (R-06) */
	public boolean isEditable() {
		throw new UnsupportedOperationException("TODO(경매)");
	}

	@Builder
	private Auction(
			User seller,
			Category category,
			String title,
			String description,
			long startPrice,
			Long buyNowPrice,
			LocalDateTime startAt,
			int durationDays,
			boolean extendEnabled,
			int extendTriggerMinutes,
			int extendMinutes) {
		this.seller = seller;
		this.category = category;
		this.title = title;
		this.description = description;
		this.startPrice = startPrice;
		this.buyNowPrice = buyNowPrice;
		this.currentPrice = startPrice;
		this.highestBidder = null;
		this.bidCount = 0;
		this.status = AuctionStatus.ACTIVE;
		this.startAt = startAt;
		this.endAt = startAt.plusDays(durationDays);
		this.originalEndAt = this.endAt;
		this.extendEnabled = extendEnabled;
		this.extendTriggerMinutes = extendTriggerMinutes;
		this.extendMinutes = extendMinutes;
		this.extendCount = 0;
	}

	public void addImage(String url, int sortOrder) {
		images.add(AuctionImage.of(this, url, sortOrder));
	}
}
