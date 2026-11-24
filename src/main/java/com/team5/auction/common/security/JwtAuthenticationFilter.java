package com.team5.auction.common.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * TODO(회원·공통): Authorization: Bearer {token} 헤더를 읽어
 * jwtTokenProvider.parse(token)로 LoginUser를 만들고 SecurityContext에 넣습니다.
 * 권한은 "ROLE_" + loginUser.role().name() 으로 부여합니다.
 * 지금은 아무것도 하지 않고 다음 필터로 넘깁니다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		chain.doFilter(request, response);
	}
}
