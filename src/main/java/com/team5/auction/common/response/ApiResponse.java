package com.team5.auction.common.response;

import java.util.List;

import com.team5.auction.common.exception.ErrorCode;

public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null, null);
	}

	public static ApiResponse<Void> fail(ErrorCode code) {
		return fail(code, code.message(), List.of());
	}

	public static ApiResponse<Void> fail(ErrorCode code, String message) {
		return fail(code, message, List.of());
	}

	public static ApiResponse<Void> fail(ErrorCode code, String message, List<FieldError> fieldErrors) {
		return new ApiResponse<>(false, null, new ErrorBody(code.code(), message, fieldErrors));
	}

	public record ErrorBody(String code, String message, List<FieldError> fieldErrors) {
	}

	public record FieldError(String field, String reason) {
	}
}
