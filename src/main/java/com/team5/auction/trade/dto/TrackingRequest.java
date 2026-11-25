package com.team5.auction.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.io.Serializable;

/**
 * 송장 입력 요청. 판매자만 입력할 수 있고, 거래가 PAID 상태일 때만 가능합니다. (R-51)
 * 입력하면 거래가 SHIPPED로 바뀌고 자동 구매확정 시각이 정해집니다. (R-52)
 */
public record TrackingRequest(
        /** 송장번호. 10~14자리 문자열입니다. */
        @Size(message = "INVALID_INPUT", min = 10, max = 14)
        @NotBlank
        String trackingNumber
){}
