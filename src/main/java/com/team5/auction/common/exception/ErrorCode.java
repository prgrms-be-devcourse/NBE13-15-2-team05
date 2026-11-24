package com.team5.auction.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러코드. 도메인마다 enum을 따로 만들어 이 인터페이스를 구현합니다. (계약서 1.3)
 * code()는 enum 이름을 그대로 씁니다.
 */
public interface ErrorCode {

	HttpStatus status();

	String code();

	String message();
}
