package com.team5.auction.user.Controller;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.security.JwtTokenProvider;
import com.team5.auction.common.security.SecurityConfig;
import com.team5.auction.common.security.SecurityErrorHandler;
import com.team5.auction.user.controller.AuthController;
import com.team5.auction.user.dto.SignupResponse;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 슬라이스 테스트.
 * 컨트롤러 + @Valid + GlobalExceptionHandler + 실제 SecurityConfig까지만 띄우고, 서비스는 가짜로 둡니다.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // JwtAuthenticationFilter(@Component Filter)가 슬라이스에 포함되면서 필요로 합니다.
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String VALID_BODY = """
		{
		  "email": "user@test.com",
		  "password": "password123",
		  "nickname": "경매왕",
		  "phone": "010-1234-5678"
		}
		""";

    @Test
    void 토큰_없이_가입하면_201과_userId를_ApiResponse_형식으로_응답한다() throws Exception {
        given(authService.signup(any())).willReturn(new SignupResponse(1L));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void 이메일_형식이_틀리면_400_INVALID_INPUT과_필드_오류를_응답하고_서비스를_호출하지_않는다() throws Exception {
        String body = VALID_BODY.replace("user@test.com", "not-an-email");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("email"));

        then(authService).should(never()).signup(any());
    }

    @Test
    void 서비스가_중복_예외를_던지면_409_EMAIL_DUPLICATED() throws Exception {
        given(authService.signup(any())).willThrow(new BusinessException(UserErrorCode.EMAIL_DUPLICATED));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_DUPLICATED"));
    }
}
