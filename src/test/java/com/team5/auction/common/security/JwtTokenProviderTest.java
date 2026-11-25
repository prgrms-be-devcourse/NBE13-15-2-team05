package com.team5.auction.common.security;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.exception.CommonErrorCode;
import com.team5.auction.user.domain.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3600);

    @ParameterizedTest
    @EnumSource(Role.class)
    void 발급한_토큰을_검증하면_같은_id와_role이_나온다(Role role) {
        String token = provider.createAccessToken(new LoginUser(7L, role));

        LoginUser parsed = provider.parse(token);

        assertThat(parsed).isEqualTo(new LoginUser(7L, role));
    }

    @Test
    void 만료된_토큰은_UNAUTHORIZED() {
        // 만료 시간을 음수로 주면 발급 순간 이미 만료된 토큰이 만들어집니다.
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -60);
        String token = expiredProvider.createAccessToken(new LoginUser(1L, Role.USER));

        assertUnauthorized(() -> provider.parse(token));
    }

    @Test
    void 다른_시크릿으로_서명한_토큰은_UNAUTHORIZED() {
        JwtTokenProvider other = new JwtTokenProvider("another-secret-key-also-at-least-32-bytes", 3600);
        String token = other.createAccessToken(new LoginUser(1L, Role.ADMIN));

        assertUnauthorized(() -> provider.parse(token));
    }

    @Test
    void payload를_바꿔치기한_토큰은_UNAUTHORIZED() {
        // 일반 유저 토큰의 서명은 그대로 두고, payload만 운영자 토큰의 것으로 교체합니다.
        String[] user = provider.createAccessToken(new LoginUser(1L, Role.USER)).split("\\.");
        String[] admin = provider.createAccessToken(new LoginUser(2L, Role.ADMIN)).split("\\.");
        String forged = user[0] + "." + admin[1] + "." + user[2];

        assertUnauthorized(() -> provider.parse(forged));
    }

    @Test
    void null_공백_JWT가_아닌_문자열은_UNAUTHORIZED() {
        assertUnauthorized(() -> provider.parse(null));
        assertUnauthorized(() -> provider.parse("   "));
        assertUnauthorized(() -> provider.parse("not-a-jwt"));
    }

    @Test
    void role_클레임이_없는_토큰은_UNAUTHORIZED() {
        String token = Jwts.builder()
                .subject("1")
                .claim(JwtTokenProvider.CLAIM_TYPE, JwtTokenProvider.TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertUnauthorized(() -> provider.parse(token));
    }

    private void assertUnauthorized(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
