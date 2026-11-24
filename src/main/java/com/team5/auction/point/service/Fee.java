package com.team5.auction.point.service;

import java.math.BigDecimal;

/** 수수료 계산 결과. Trade에 스냅샷으로 저장합니다. (R-54) */
public record Fee(BigDecimal rate, long feeAmount, long settleAmount) {
}
