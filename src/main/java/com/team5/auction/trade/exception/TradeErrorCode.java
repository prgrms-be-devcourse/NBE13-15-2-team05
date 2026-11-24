package com.team5.auction.trade.exception;

import org.springframework.http.HttpStatus;

import com.team5.auction.common.exception.ErrorCode;

public enum TradeErrorCode implements ErrorCode {

	TRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "거래를 찾을 수 없습니다."),
	TRADE_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "거래 당사자가 아닙니다."),
	TRADE_INVALID_STATUS(HttpStatus.CONFLICT, "현재 거래 상태에서는 처리할 수 없습니다.");  // R-55

	private final HttpStatus status;
	private final String message;

	TradeErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public String message() {
		return message;
	}
}
