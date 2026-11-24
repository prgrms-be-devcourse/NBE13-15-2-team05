package com.team5.auction.common.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 auction.* 정책 설정값. (계약서 1.6)
 * 구간은 maxPrice 오름차순, maxPrice는 "이하"(경계 포함), 마지막 구간의 maxPrice는 null(상한 없음).
 */
@ConfigurationProperties(prefix = "auction")
public record AuctionPolicyProperties(
	Fee fee,
	BidUnit bidUnit,
	Extension extension,
	TradePolicy trade,
	PointPolicy point
) {

	public record Fee(List<FeeTier> tiers) {
		public Fee {
			validateTiers(tiers, FeeTier::maxPrice, "auction.fee.tiers");
		}
	}

	public record FeeTier(Long maxPrice, BigDecimal rate) {
	}

	public record BidUnit(List<BidUnitTier> tiers) {
		public BidUnit {
			validateTiers(tiers, BidUnitTier::maxPrice, "auction.bid-unit.tiers");
		}
	}

	public record BidUnitTier(Long maxPrice, long unit) {
	}

	public record Extension(boolean enabled, int triggerMinutes, int extendMinutes) {
	}

	public record TradePolicy(int shipDeadlineDays, int autoConfirmDays) {
	}

	public record PointPolicy(long maxChargeAmount) {
	}

	private static <T> void validateTiers(List<T> tiers, java.util.function.Function<T, Long> maxPrice, String name) {
		if (tiers == null || tiers.isEmpty()) {
			throw new IllegalArgumentException(name + " 구간이 비어 있습니다.");
		}
		for (int i = 0; i < tiers.size(); i++) {
			Long max = maxPrice.apply(tiers.get(i));
			boolean last = i == tiers.size() - 1;
			if (last && max != null) {
				throw new IllegalArgumentException(name + " 마지막 구간의 max-price는 비워야 합니다.");
			}
			if (!last && max == null) {
				throw new IllegalArgumentException(name + " 마지막이 아닌 구간에 max-price가 없습니다.");
			}
			if (!last && i > 0 && max <= maxPrice.apply(tiers.get(i - 1))) {
				throw new IllegalArgumentException(name + " max-price는 오름차순이어야 합니다.");
			}
		}
	}
}
