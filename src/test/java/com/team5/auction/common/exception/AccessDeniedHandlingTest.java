package com.team5.auction.common.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team5.auction.common.security.JwtTokenProvider;
import com.team5.auction.common.security.SecurityConfig;
import com.team5.auction.common.security.SecurityErrorHandler;

/**
 * 컨트롤러 안에서 난 권한 거부(@PreAuthorize 등)가 500이 아니라
 * URL 권한 검사와 같은 401·403으로 나가는지. 실제 SecurityConfig를 띄워 확인한다.
 */
@WebMvcTest(AccessDeniedHandlingTest.DeniedController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, AccessDeniedHandlingTest.DeniedController.class})
class AccessDeniedHandlingTest {

	@RestController
	static class DeniedController {

		/** 로그인이 필요한 경로 */
		@GetMapping("/api/test/denied")
		void denied() {
			throw new AccessDeniedException("denied");
		}

		/** permitAll 경로 (비로그인으로 컨트롤러까지 들어오는 경우) */
		@GetMapping("/api/auth/test-denied")
		void deniedOnPublicPath() {
			throw new AccessDeniedException("denied");
		}
	}

	@Autowired
	private MockMvc mockMvc;

	// JwtAuthenticationFilter(@Component Filter)가 슬라이스에 포함되면서 필요로 한다
	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void 로그인했지만_권한이_없으면_403_FORBIDDEN() throws Exception {
		mockMvc.perform(get("/api/test/denied").with(user("user")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void 비로그인이면_401_UNAUTHORIZED() throws Exception {
		mockMvc.perform(get("/api/auth/test-denied"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}
}
