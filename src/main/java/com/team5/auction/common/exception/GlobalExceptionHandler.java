package com.team5.auction.common.exception;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.team5.auction.common.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		ErrorCode code = e.getErrorCode();
		return ResponseEntity.status(code.status()).body(ApiResponse.fail(code, e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
		List<ApiResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
			.map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
			.toList();
		ErrorCode code = CommonErrorCode.INVALID_INPUT;
		return ResponseEntity.status(code.status()).body(ApiResponse.fail(code, code.message(), fieldErrors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
		ErrorCode code = CommonErrorCode.INVALID_INPUT;
		return ResponseEntity.status(code.status()).body(ApiResponse.fail(code));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
		log.error("Unexpected error", e);
		ErrorCode code = CommonErrorCode.INTERNAL_ERROR;
		return ResponseEntity.status(code.status()).body(ApiResponse.fail(code));
	}
}
