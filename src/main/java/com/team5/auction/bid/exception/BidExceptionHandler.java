package com.team5.auction.bid.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.team5.auction.bid.controller.BidController;
import com.team5.auction.common.response.ApiResponse;

/**
 * 동시 입찰 충돌을 409 BID_CONFLICT로 바꾼다 (R-16, 계약서 입찰 충돌).
 *
 * <p>Auction의 version 충돌은 커밋 시점에 터지므로 서비스 안에서는 잡을 수 없어 여기서 처리한다.
 * 경매 수정·취소도 같은 version을 쓰기 때문에 전역이 아니라 입찰 컨트롤러에만 적용한다.
 * 전역 처리기의 {@code Exception} 처리보다 먼저 잡히도록 우선순위를 높인다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = BidController.class)
public class BidExceptionHandler {

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiResponse<Void>> handleBidConflict(OptimisticLockingFailureException e) {
		BidErrorCode code = BidErrorCode.BID_CONFLICT;
		return ResponseEntity.status(code.status()).body(ApiResponse.fail(code));
	}
}
