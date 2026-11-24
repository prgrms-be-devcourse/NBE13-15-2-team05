package com.team5.auction.auction.exception;

import org.springframework.http.HttpStatus;

import com.team5.auction.common.exception.ErrorCode;

public enum AuctionErrorCode implements ErrorCode {

	AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."),
	AUCTION_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "경매 기간은 1일 이상 14일 이하입니다."),  // R-02
	BUY_NOW_PRICE_INVALID(HttpStatus.BAD_REQUEST, "즉시구매가는 시작가보다 커야 합니다."),  // R-03
	PRICE_UNIT_INVALID(HttpStatus.BAD_REQUEST, "가격은 100원 단위, 최소 1,000원입니다."),  // R-08
	IMAGE_COUNT_INVALID(HttpStatus.BAD_REQUEST, "이미지는 1장 이상 10장 이하입니다."),  // R-09
	AUCTION_NOT_OWNER(HttpStatus.FORBIDDEN, "본인 경매가 아닙니다."),
	AUCTION_NOT_EDITABLE(HttpStatus.CONFLICT, "입찰이 있거나 진행 중이 아닌 경매는 수정·취소할 수 없습니다.");  // R-06

	private final HttpStatus status;
	private final String message;

	AuctionErrorCode(HttpStatus status, String message) {
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
