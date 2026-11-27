package com.team5.auction.point.service;

/**
 * 수수료 계산기. 담당: 포인트 (계약서 3.2)
 * feeAmount = price × rate, 원 단위 미만 버림 (BigDecimal.setScale(0, RoundingMode.DOWN))
 * settleAmount = price - feeAmount
 * double로 계산하지 않습니다. 100 * 0.29가 28.999…가 되어 28원으로 버려집니다.
 */
public interface FeeCalculator {

	/** 거래 성립 시 호출합니다. */
	Fee calculate(long price);
}
