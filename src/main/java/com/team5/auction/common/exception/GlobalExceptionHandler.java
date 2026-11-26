package com.team5.auction.common.exception;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.team5.auction.common.response.ApiResponse;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리기.
 *
 * <p>스프링 MVC 표준 예외(잘못된 파라미터, 필수값 누락, 없는 URL, 지원하지 않는 메서드 등)는
 * 부모 {@link ResponseEntityExceptionHandler}가 맞는 상태 코드로 처리하고,
 * 여기서는 {@link #handleExceptionInternal}에서 응답 본문만 {@link ApiResponse} 형식으로 바꾼다.
 * 이렇게 하지 않으면 아래 {@code Exception} 처리가 이 예외들을 먼저 가로채 500으로 응답한다.
 *
 * <p>부모가 이미 처리하는 예외(MethodArgumentNotValidException 등)에 {@code @ExceptionHandler}를
 * 다시 붙이면 매핑이 겹쳐 앱이 뜨지 않으므로, 그런 예외는 handleExceptionInternal에서 다룬다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		ErrorCode code = e.getErrorCode();
		return ResponseEntity.status(code.status()).body(ApiResponse.fail(code, e.getMessage()));
	}

	/**
	 * Bean Validation 위반 (jakarta).
	 * 컨트롤러에 {@code @Validated}를 붙이면 {@code @RequestParam} 검증 실패가 HandlerMethodValidationException이 아니라
	 * 이 예외로 나오므로 같은 400으로 맞춘다. 서비스·엔티티 검증에서 나온 경우는 입력을 앞에서 못 거른 것이라 경고 로그를 남긴다.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
		log.warn("Constraint violation: {}", e.getMessage());
		List<ApiResponse.FieldError> fieldErrors = e.getConstraintViolations().stream()
			.map(v -> new ApiResponse.FieldError(lastNodeName(v.getPropertyPath()), v.getMessage()))
			.toList();
		ErrorCode code = CommonErrorCode.INVALID_INPUT;
		return ResponseEntity.status(code.status()).body(ApiResponse.fail(code, code.message(), fieldErrors));
	}

	/**
	 * 동시 수정 충돌.
	 * 낙관적 락(@Version) 충돌과 비관적 락 대기 시간 초과가 모두 여기로 온다 (둘 다 ConcurrencyFailureException 하위).
	 * 입찰은 BidExceptionHandler가 먼저 잡아 BID_CONFLICT로 응답하고, 여기는 그 밖의 API(경매 수정·취소 등)를 맡는다.
	 */
	@ExceptionHandler(ConcurrencyFailureException.class)
	public ResponseEntity<ApiResponse<Void>> handleConcurrencyFailure(ConcurrencyFailureException e) {
		log.warn("Concurrency failure: {}", e.getMessage());
		ErrorCode code = CommonErrorCode.CONCURRENT_UPDATE;
		return ResponseEntity.status(code.status()).body(ApiResponse.fail(code));
	}

	/**
	 * 컨트롤러 안에서 난 권한 거부 (예: 나중에 @PreAuthorize를 쓰는 경우).
	 * 여기서 응답하지 않고 다시 던져 시큐리티 필터(ExceptionTranslationFilter)가 처리하게 한다.
	 * 그래야 비로그인은 401, 로그인했지만 권한 없음은 403으로 URL 권한 검사와 똑같이 나간다 (SecurityErrorHandler).
	 * 이렇게 하지 않으면 아래 Exception 처리가 잡아 500으로 응답한다.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public void handleAccessDenied(AccessDeniedException e) {
		throw e;
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
		log.error("Unexpected error", e);
		ErrorCode code = CommonErrorCode.INTERNAL_ERROR;
		return ResponseEntity.status(code.status()).body(ApiResponse.fail(code));
	}

	/** 스프링 MVC 표준 예외의 응답 본문을 ApiResponse 형식으로 바꾼다. 상태 코드는 스프링이 정한 값을 따른다. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
		Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

		ErrorCode code = toErrorCode(statusCode);
		if (statusCode.is5xxServerError()) {
			log.error("Spring MVC error", ex);
		}
		ApiResponse<Void> response = ApiResponse.fail(code, code.message(), fieldErrors(ex));
		return ResponseEntity.status(statusCode).headers(headers).body(response);
	}

	private ErrorCode toErrorCode(HttpStatusCode statusCode) {
		if (statusCode.is5xxServerError()) {
			return CommonErrorCode.INTERNAL_ERROR;
		}
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		if (status == null) {
			return CommonErrorCode.INVALID_INPUT;
		}
		return switch (status) {
			case NOT_FOUND -> CommonErrorCode.NOT_FOUND;
			case METHOD_NOT_ALLOWED -> CommonErrorCode.METHOD_NOT_ALLOWED;
			case UNSUPPORTED_MEDIA_TYPE -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
			// 그 밖의 4xx는 모두 클라이언트 요청 문제이므로 INVALID_INPUT으로 묶는다
			default -> CommonErrorCode.INVALID_INPUT;
		};
	}

	/** 어떤 입력이 잘못됐는지 프론트가 알 수 있도록 필드(파라미터) 이름과 사유를 담는다. */
	private List<ApiResponse.FieldError> fieldErrors(Exception ex) {
		if (ex instanceof MethodArgumentNotValidException e) {
			return e.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		}
		if (ex instanceof TypeMismatchException e) {
			return List.of(new ApiResponse.FieldError(e.getPropertyName(), typeMismatchReason(e)));
		}
		if (ex instanceof MissingServletRequestParameterException e) {
			return List.of(new ApiResponse.FieldError(e.getParameterName(), "필수 값입니다."));
		}
		if (ex instanceof HandlerMethodValidationException e) {
			return e.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
					.map(error -> new ApiResponse.FieldError(
						result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
				.toList();
		}
		return List.of();
	}

	/** "getTrades.size"처럼 메서드 이름이 앞에 붙은 경로에서 파라미터(필드) 이름만 꺼낸다 */
	private String lastNodeName(Path path) {
		String name = null;
		for (Path.Node node : path) {
			name = node.getName();
		}
		return name;
	}

	/** enum이면 받을 수 있는 값을 알려 준다 (예: role=abc → BUYER, SELLER) */
	private String typeMismatchReason(TypeMismatchException e) {
		Class<?> type = e.getRequiredType();
		if (type != null && type.isEnum()) {
			List<String> allowed = Arrays.stream(type.getEnumConstants()).map(Object::toString).toList();
			return "허용되지 않는 값입니다. 가능한 값: " + String.join(", ", allowed);
		}
		return "형식이 올바르지 않습니다.";
	}
}
