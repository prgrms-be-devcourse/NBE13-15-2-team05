package com.team5.auction.common.security;

import com.team5.auction.user.domain.Role;

/**
 * JWT 필터가 SecurityContext에 넣는 로그인 유저. (계약서 1.4)
 * 컨트롤러에서는 @AuthenticationPrincipal LoginUser loginUser 로만 받습니다.
 */
public record LoginUser(Long id, Role role) {
}
