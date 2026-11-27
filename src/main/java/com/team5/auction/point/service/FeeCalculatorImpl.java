package com.team5.auction.point.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.common.config.AuctionPolicyProperties.FeeTier;

import lombok.RequiredArgsConstructor;

/** 거래 금액이 속한 구간의 수수료율로 수수료와 정산금을 계산 (R-54) */
@Component
@RequiredArgsConstructor
public class FeeCalculatorImpl implements FeeCalculator {

	private final AuctionPolicyProperties properties;

	@Override
	public Fee calculate(long price) {
		BigDecimal rate = rateFor(price);
		// double 대신 BigDecimal로 곱하고, 원 단위 미만은 버린다 (계약서 3.2)
		long feeAmount = BigDecimal.valueOf(price)
				.multiply(rate)
				.setScale(0, RoundingMode.DOWN)
				.longValue();
		return new Fee(rate, feeAmount, price - feeAmount);
	}

	/** 금액이 속한 구간의 수수료율. maxPrice 이하면 그 구간이고, 마지막 구간(maxPrice == null)은 상한이 없다 */
	private BigDecimal rateFor(long price) {
		for (FeeTier tier : properties.fee().tiers()) {
			if (tier.maxPrice() == null || price <= tier.maxPrice()) {
				return tier.rate();
			}
		}
		// 시작할 때 설정 검증(마지막 구간은 maxPrice == null)이 있어서 여기까지 오면 설정 버그
		throw new IllegalStateException("수수료 구간을 찾지 못했습니다. price=" + price);
	}
}
