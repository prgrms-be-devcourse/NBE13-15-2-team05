package com.team5.auction.point.exception;

import org.springframework.http.HttpStatus;

import com.team5.auction.common.exception.ErrorCode;

public enum PointErrorCode implements ErrorCode {

	CHARGE_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "충전 금액이 올바르지 않습니다."),  // R-23
	BALANCE_INSUFFICIENT(HttpStatus.CONFLICT, "사용 가능한 포인트가 부족합니다.");  // R-14

	private final HttpStatus status;
	private final String message;

	PointErrorCode(HttpStatus status, String message) {
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
