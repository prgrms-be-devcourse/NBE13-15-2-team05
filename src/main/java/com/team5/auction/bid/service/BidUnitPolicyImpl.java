package com.team5.auction.bid.service;

import org.springframework.stereotype.Component;

import com.team5.auction.common.config.AuctionPolicyProperties;

import lombok.RequiredArgsConstructor;

/** TODO(입찰): properties.bidUnit().tiers()로 구간을 찾아 반환 */
@Component
@RequiredArgsConstructor
public class BidUnitPolicyImpl implements BidUnitPolicy {

	private final AuctionPolicyProperties properties;

	@Override
	public long unitFor(long currentPrice) {
		throw new UnsupportedOperationException("TODO(입찰)");
	}
}
