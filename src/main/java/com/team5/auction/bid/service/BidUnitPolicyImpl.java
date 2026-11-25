package com.team5.auction.bid.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.common.config.AuctionPolicyProperties.BidUnitTier;

import lombok.RequiredArgsConstructor;

/** 현재가가 속한 구간의 입찰 단위를 반환 (R-13) */
@Component
@RequiredArgsConstructor
public class BidUnitPolicyImpl implements BidUnitPolicy {

	private final AuctionPolicyProperties properties;

	@Override
	public long unitFor(long currentPrice) {
		List<BidUnitTier> tiers = properties.bidUnit().tiers();

		for (BidUnitTier tier : tiers)
			if (tier.maxPrice() == null || currentPrice <= tier.maxPrice())
				return tier.unit();

		// 시작 시 검증(마지막 구간 maxPrice == null)이 있어 여기까지 오면 설정 버그
		throw new IllegalStateException("입찰 단위 구간을 찾지 못했습니다: " + currentPrice);
	}
}
