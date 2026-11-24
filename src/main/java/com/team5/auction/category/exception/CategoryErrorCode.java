package com.team5.auction.category.exception;

import org.springframework.http.HttpStatus;

import com.team5.auction.common.exception.ErrorCode;

public enum CategoryErrorCode implements ErrorCode {

	CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
	CATEGORY_NAME_DUPLICATED(HttpStatus.CONFLICT, "이미 있는 카테고리 이름입니다."),
	CATEGORY_IN_USE(HttpStatus.CONFLICT, "경매가 등록된 카테고리는 삭제할 수 없습니다.");  // R-64

	private final HttpStatus status;
	private final String message;

	CategoryErrorCode(HttpStatus status, String message) {
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
