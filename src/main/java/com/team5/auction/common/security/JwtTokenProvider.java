package com.team5.auction.common.security;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.exception.CommonErrorCode;
import com.team5.auction.user.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * TODO(회원·공통): JWT 발급·검증 구현.
 * - 라이브러리(jjwt 등) 의존성과 application.yml의 jwt.* 설정 키는 구현할 때 추가합니다.
 */
@Component
public class JwtTokenProvider {

	public static final String TYPE_ACCESS = "access";
	public static final String CLAIM_TYPE = "tokenType";
	public static final String CLAIM_ROLE = "role";

	private final SecretKey key;
	private final long accessSeconds;

	public JwtTokenProvider(
            @Value("${jwt.secret}")String secret,
			@Value("${jwt.access-seconds}") long accessSeconds) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessSeconds = accessSeconds;
	}

	/** 로그인 성공 시 액세스 토큰 발급. */
	public String createAccessToken(LoginUser loginUser) {
		return compact(loginUser, TYPE_ACCESS, this.accessSeconds);
	}

	/** 토큰 검증 후 LoginUser 복원. 유효하지 않으면 BusinessException(UNAUTHORIZED). */
	public LoginUser parse(String token) {
		if (token == null || token.isBlank()) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}
		try {
			Claims claims =
					Jwts.parser().verifyWith(this.key).build().parseSignedClaims(token).getPayload();
			Object type = claims.get(CLAIM_TYPE);

			if (type == null || !type.toString().equals(TYPE_ACCESS)) {
				throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
			}

			Object role = claims.get(CLAIM_ROLE);
			if (role == null) {
				throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
			}

			Long userId = Long.valueOf(claims.getSubject());

			return new LoginUser(userId, Role.valueOf(role.toString()));
		} catch (JwtException | IllegalArgumentException ex) {
			throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
		}
	}

	private String compact(LoginUser loginUser, String tokenType, long seconds) {
		Instant now = Instant.now();

		return Jwts.builder()
				.subject(Long.toString(loginUser.id()))
				.id(java.util.UUID.randomUUID().toString())
				.claim(CLAIM_TYPE, tokenType)
				.claim(CLAIM_ROLE, loginUser.role())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(seconds)))
				.signWith(this.key, Jwts.SIG.HS256)
				.compact();
	}

	public long getAccessSeconds(){
		return accessSeconds;
	}
}
