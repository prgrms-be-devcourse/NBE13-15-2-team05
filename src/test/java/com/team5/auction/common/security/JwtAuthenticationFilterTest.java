package com.team5.auction.common.security;

import com.team5.auction.user.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3600);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이면_LoginUser와_권한이_SecurityContext에_들어간다() throws Exception {
        String token = provider.createAccessToken(new LoginUser(7L, Role.ADMIN));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWith("Bearer " + token), new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo(new LoginUser(7L, Role.ADMIN));
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        assertThat(chain.getRequest()).isNotNull(); // 다음 필터로 넘어감
    }

    @Test
    void 헤더가_없으면_인증_없이_다음_필터로_넘어간다() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 유효하지_않은_토큰이면_예외_없이_인증_없이_넘어간다() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWith("Bearer not-a-jwt"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void Bearer_형식이_아니면_무시한다() throws Exception {
        String token = provider.createAccessToken(new LoginUser(1L, Role.USER));

        filter.doFilter(requestWith(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }
}