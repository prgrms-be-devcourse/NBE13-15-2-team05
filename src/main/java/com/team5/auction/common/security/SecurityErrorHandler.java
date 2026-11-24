package com.team5.auction.common.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.exception.CommonErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 시큐리티에서 난 401·403도 GlobalExceptionHandler를 거쳐 같은 ApiResponse 형식으로 응답합니다.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final HandlerExceptionResolver resolver;

	public SecurityErrorHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) {
		resolver.resolveException(request, response, null, new BusinessException(CommonErrorCode.UNAUTHORIZED));
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) {
		resolver.resolveException(request, response, null, new BusinessException(CommonErrorCode.FORBIDDEN));
	}
}
