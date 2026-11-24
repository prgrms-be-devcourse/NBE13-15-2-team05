package com.team5.auction.user.exception;

import org.springframework.http.HttpStatus;

import com.team5.auction.common.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {

	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
	ADDRESS_REQUIRED_WHILE_LOCKED(HttpStatus.CONFLICT, "입찰 중인 포인트가 있으면 배송지를 비울 수 없습니다.");  // R-66

	private final HttpStatus status;
	private final String message;

	UserErrorCode(HttpStatus status, String message) {
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
