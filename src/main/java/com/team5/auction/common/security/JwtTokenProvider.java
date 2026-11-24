package com.team5.auction.common.security;

import org.springframework.stereotype.Component;

/**
 * TODO(회원·공통): JWT 발급·검증 구현.
 * - 라이브러리(jjwt 등) 의존성과 application.yml의 jwt.* 설정 키는 구현할 때 추가합니다.
 */
@Component
public class JwtTokenProvider {

	/** 로그인 성공 시 액세스 토큰 발급. */
	public String createAccessToken(LoginUser loginUser) {
		throw new UnsupportedOperationException("TODO: JWT 발급");
	}

	/** 토큰 검증 후 LoginUser 복원. 유효하지 않으면 BusinessException(UNAUTHORIZED). */
	public LoginUser parse(String token) {
		throw new UnsupportedOperationException("TODO: JWT 검증");
	}
}
