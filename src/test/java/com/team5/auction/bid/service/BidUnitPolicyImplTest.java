package com.team5.auction.bid.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.common.config.AuctionPolicyProperties.BidUnit;
import com.team5.auction.common.config.AuctionPolicyProperties.BidUnitTier;

/** 입찰 단위 구간 경계 (R-13). 스프링 없이 application.yml과 같은 구간으로 직접 만든다. */
class BidUnitPolicyImplTest {

	private final BidUnitPolicy policy = new BidUnitPolicyImpl(new AuctionPolicyProperties(
		null,
		new BidUnit(List.of(
			new BidUnitTier(9_999L, 500),
			new BidUnitTier(99_999L, 1_000),
			new BidUnitTier(999_999L, 5_000),
			new BidUnitTier(null, 10_000))),
		null, null, null));

	@ParameterizedTest(name = "현재가 {0} → 단위 {1}")
	@CsvSource({
		"0, 500",
		"9999, 500",
		"10000, 1000",
		"99999, 1000",
		"100000, 5000",
		"999999, 5000",
		"1000000, 10000",
		"50000000, 10000"
	})
	void 현재가가_속한_구간의_단위를_반환한다(long currentPrice, long expectedUnit) {
		assertThat(policy.unitFor(currentPrice)).isEqualTo(expectedUnit);
	}
}
