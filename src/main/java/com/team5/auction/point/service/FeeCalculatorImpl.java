package com.team5.auction.point.service;

import org.springframework.stereotype.Component;

import com.team5.auction.common.config.AuctionPolicyProperties;

import lombok.RequiredArgsConstructor;

/** TODO(포인트): properties.fee().tiers()로 구간을 찾아 계산 */
@Component
@RequiredArgsConstructor
public class FeeCalculatorImpl implements FeeCalculator {

	private final AuctionPolicyProperties properties;

	@Override
	public Fee calculate(long price) {
		throw new UnsupportedOperationException("TODO(포인트)");
	}
}
