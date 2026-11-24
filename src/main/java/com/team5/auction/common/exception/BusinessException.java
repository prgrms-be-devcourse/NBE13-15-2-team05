package com.team5.auction.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	/** 메시지에 금액 등 구체 값을 넣을 때. 예: "최소 입찰가는 15,000원입니다." */
	public BusinessException(ErrorCode errorCode, String detailMessage) {
		super(detailMessage);
		this.errorCode = errorCode;
	}
}
