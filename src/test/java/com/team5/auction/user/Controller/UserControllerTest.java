package com.team5.auction.user.Controller;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.security.JwtTokenProvider;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.common.security.SecurityConfig;
import com.team5.auction.common.security.SecurityErrorHandler;
import com.team5.auction.user.controller.UserController;
import com.team5.auction.user.domain.Role;
import com.team5.auction.user.dto.MeResponse;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 슬라이스 테스트.
 * 실제 provider로 발급한 토큰을 헤더에 넣어, 필터가 만든 LoginUser가 컨트롤러까지 전달되는지 확인합니다.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-must-be-at-least-32-bytes-long",
        "jwt.access-seconds=3600"
})
class UserControllerTest {

    private static final Long USER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserService userService;

    private final MeResponse myInfo =
            new MeResponse(USER_ID, "user@test.com", "경매왕", "010-1234-5678", Role.USER, 10_000L, 0L, null);

    private String bearer() {
        return "Bearer " + jwtTokenProvider.createAccessToken(new LoginUser(USER_ID, Role.USER));
    }

    private ResultActions patchMe(String json) throws Exception {
        return mockMvc.perform(patch("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    // ===== GET /api/users/me =====

    @Test
    void 토큰으로_조회하면_토큰의_userId로_서비스를_호출하고_200() throws Exception {
        given(userService.me(USER_ID)).willReturn(myInfo);

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(USER_ID))
                .andExpect(jsonPath("$.data.nickname").value("경매왕"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.availableBalance").value(10_000))
                .andExpect(jsonPath("$.data.address").value(nullValue()));

        then(userService).should().me(USER_ID);
    }

    @Test
    void 토큰_없이_조회하면_401이고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        then(userService).should(never()).me(any());
    }

    // ===== PATCH /api/users/me : 정상 =====

    @Test
    void 수정하면_토큰의_userId로_서비스를_호출하고_200() throws Exception {
        given(userService.updateMe(eq(USER_ID), any())).willReturn(myInfo);

        patchMe("""
                { "phone": "010-1234-5678" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"));

        then(userService).should().updateMe(eq(USER_ID), any());
    }

    @Test
    void clearAddress_없이_일부_필드만_보내도_200() throws Exception {
        // Jackson 3에서 기본 타입 boolean이면 역직렬화가 실패하던 문제의 회귀 테스트
        given(userService.updateMe(eq(USER_ID), any())).willReturn(myInfo);

        patchMe("""
                { "nickname": "경매왕" }
                """)
                .andExpect(status().isOk());
    }

    // ===== PATCH /api/users/me : 검증 실패 =====

    @Test
    void 닉네임이_2자_미만이면_400_INVALID_INPUT_서비스를_호출하지_않는다() throws Exception {
        patchMe("""
                { "nickname": "a" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("nickname"));

        then(userService).should(never()).updateMe(any(), any());
    }

    @Test
    void 전화번호_형식이_틀리면_400() throws Exception {
        patchMe("""
                { "phone": "01012345678" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("phone"));
    }

    @Test
    void 배송지의_우편번호가_빠지면_400_중첩_필드_오류() throws Exception {
        patchMe("""
                { "address": { "recipientName": "홍길동", "recipientPhone": "010-2222-3333",
                               "address1": "서울시 강남구 테헤란로 1" } }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("address.zipCode"));

        then(userService).should(never()).updateMe(any(), any());
    }

    @Test
    void 배송지의_우편번호가_5자리_숫자가_아니면_400() throws Exception {
        patchMe("""
                { "address": { "recipientName": "홍길동", "recipientPhone": "010-2222-3333",
                               "zipCode": "1234", "address1": "서울시 강남구 테헤란로 1" } }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("address.zipCode"));
    }

    @Test
    void 받는_사람_전화번호_형식이_틀리면_400() throws Exception {
        patchMe("""
                { "address": { "recipientName": "홍길동", "recipientPhone": "01022223333",
                               "zipCode": "06236", "address1": "서울시 강남구 테헤란로 1" } }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("address.recipientPhone"));
    }

    // ===== PATCH /api/users/me : 서비스 예외 =====

    @Test
    void 서비스가_NICKNAME_DUPLICATED를_던지면_409() throws Exception {
        given(userService.updateMe(eq(USER_ID), any()))
                .willThrow(new BusinessException(UserErrorCode.NICKNAME_DUPLICATED));

        patchMe("""
                { "nickname": "새닉네임" }
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NICKNAME_DUPLICATED"));
    }

    @Test
    void 서비스가_ADDRESS_REQUIRED_WHILE_LOCKED를_던지면_409() throws Exception {
        given(userService.updateMe(eq(USER_ID), any()))
                .willThrow(new BusinessException(UserErrorCode.ADDRESS_REQUIRED_WHILE_LOCKED));

        patchMe("""
                { "clearAddress": true }
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ADDRESS_REQUIRED_WHILE_LOCKED"));
    }
}
