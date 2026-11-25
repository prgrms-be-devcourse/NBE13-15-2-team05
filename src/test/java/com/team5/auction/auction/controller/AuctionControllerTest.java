package com.team5.auction.auction.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.service.AuctionService;
import com.team5.auction.common.security.JwtTokenProvider;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.common.security.SecurityConfig;
import com.team5.auction.common.security.SecurityErrorHandler;
import com.team5.auction.user.domain.Role;

/** 경매 등록 API의 응답 형식(201 + ApiResponse)과 요청 검증(400)을 확인합니다. DB 없이 돕니다. */
@WebMvcTest(AuctionController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class})
class AuctionControllerTest {

	private static final String VALID_BODY = """
		{
		  "categoryId": 10,
		  "title": "맥북 프로",
		  "description": "거의 새것",
		  "startPrice": 10000,
		  "buyNowPrice": 50000,
		  "durationDays": 7,
		  "imageUrls": ["https://example.com/a.jpg"]
		}
		""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuctionService auctionService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void 경매를_등록하면_201과_경매ID를_돌려준다() throws Exception {
		Auction saved = mock(Auction.class);
		given(saved.getId()).willReturn(1L);
		given(auctionService.create(eq(7L), any())).willReturn(saved);

		mockMvc.perform(post("/api/auctions")
				.with(loginUser(7L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.auctionId").value(1));
	}

	@Test
	void 필수값이_빠지면_400과_필드별_오류를_돌려준다() throws Exception {
		String body = """
			{
			  "title": "",
			  "description": "거의 새것",
			  "startPrice": 10000,
			  "durationDays": 7
			}
			""";

		mockMvc.perform(post("/api/auctions")
				.with(loginUser(7L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'categoryId')]").exists())
			.andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'title')]").exists());
		then(auctionService).should(never()).create(any(), any());
	}

	@Test
	void 가격이_0_이하면_400을_돌려준다() throws Exception {
		String body = VALID_BODY.replace("\"startPrice\": 10000", "\"startPrice\": 0");

		mockMvc.perform(post("/api/auctions")
				.with(loginUser(7L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'startPrice')]").exists());
	}

	@Test
	void 이미지가_10장을_넘으면_400을_돌려준다() throws Exception {
		String elevenImages = String.join(",", java.util.Collections.nCopies(11, "\"https://example.com/a.jpg\""));
		String body = VALID_BODY.replace("[\"https://example.com/a.jpg\"]", "[" + elevenImages + "]");

		mockMvc.perform(post("/api/auctions")
				.with(loginUser(7L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.fieldErrors[?(@.field == 'imageUrls')]").exists());
	}

	@Test
	void 로그인하지_않으면_401을_돌려준다() throws Exception {
		mockMvc.perform(post("/api/auctions")
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY))
			.andExpect(status().isUnauthorized());
		then(auctionService).should(never()).create(any(), any());
	}

	/** JWT 필터가 아직 구현 전이라, 필터가 넣어줄 LoginUser를 테스트에서 직접 SecurityContext에 넣습니다. */
	private RequestPostProcessor loginUser(Long userId) {
		LoginUser loginUser = new LoginUser(userId, Role.USER);
		return authentication(new UsernamePasswordAuthenticationToken(
			loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}
}
