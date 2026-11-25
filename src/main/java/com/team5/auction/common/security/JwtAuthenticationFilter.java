package com.team5.auction.common.security;

import com.team5.auction.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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

		String header = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (header != null && header.startsWith("Bearer ")){
			try{
				String token = header.substring("Bearer ".length()).trim();
				LoginUser loginUser = this.jwtTokenProvider.parse(token);

				UsernamePasswordAuthenticationToken authentication =
						UsernamePasswordAuthenticationToken.authenticated(
								loginUser,
								null,
								List.of(new SimpleGrantedAuthority("ROLE_" + loginUser.role().name()))
						);
				SecurityContext context = SecurityContextHolder.createEmptyContext();

				context.setAuthentication(authentication);

				SecurityContextHolder.setContext(context);

			} catch (BusinessException ex){
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}
}
