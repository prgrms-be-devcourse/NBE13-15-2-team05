package com.team5.auction.notification.exception;

import org.springframework.http.HttpStatus;

import com.team5.auction.common.exception.ErrorCode;

public enum NotificationErrorCode implements ErrorCode {

	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String message;

	NotificationErrorCode(HttpStatus status, String message) {
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
