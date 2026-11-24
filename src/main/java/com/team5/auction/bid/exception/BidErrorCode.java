package com.team5.auction.bid.exception;

import org.springframework.http.HttpStatus;

import com.team5.auction.common.exception.ErrorCode;

public enum BidErrorCode implements ErrorCode {

	SELF_BID_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인 경매에는 입찰할 수 없습니다."),  // R-10
	ALREADY_HIGHEST_BIDDER(HttpStatus.CONFLICT, "이미 최고 입찰자입니다."),  // R-11
	BID_TOO_LOW(HttpStatus.BAD_REQUEST, "최소 입찰가보다 낮습니다."),  // R-12, R-13
	AUCTION_CLOSED(HttpStatus.CONFLICT, "마감된 경매입니다."),  // R-17
	ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "기본 배송지를 먼저 등록해 주세요."),  // R-19
	BUY_NOW_NOT_AVAILABLE(HttpStatus.CONFLICT, "즉시구매할 수 없는 경매입니다."),  // R-03, R-45
	BID_CONFLICT(HttpStatus.CONFLICT, "다른 입찰이 먼저 처리되었습니다. 현재가를 확인하고 다시 입찰해 주세요.");  // R-16

	private final HttpStatus status;
	private final String message;

	BidErrorCode(HttpStatus status, String message) {
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
