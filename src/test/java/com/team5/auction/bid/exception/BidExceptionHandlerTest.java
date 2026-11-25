package com.team5.auction.bid.exception;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.bid.controller.BidController;
import com.team5.auction.bid.service.BidService;
import com.team5.auction.common.exception.GlobalExceptionHandler;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.user.domain.Role;

/** 커밋 시점 version 충돌이 500이 아니라 409 BID_CONFLICT로 나가는지 (R-16) */
@ExtendWith(MockitoExtension.class)
class BidExceptionHandlerTest {

	@Mock
	private BidService bidService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		// 전역 처리기도 함께 등록해 Exception 처리보다 먼저 잡히는지까지 확인한다
		mockMvc = MockMvcBuilders.standaloneSetup(new BidController(bidService))
			.setControllerAdvice(new GlobalExceptionHandler(), new BidExceptionHandler())
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.build();
		LoginUser loginUser = new LoginUser(3L, Role.USER);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void 입찰_중_version_충돌이_나면_409_BID_CONFLICT() throws Exception {
		given(bidService.placeBid(anyLong(), anyLong(), anyLong()))
			.willThrow(new ObjectOptimisticLockingFailureException(Auction.class, 10L));

		mockMvc.perform(post("/api/auctions/10/bids")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\": 15000}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("BID_CONFLICT"));
	}

	@Test
	void 즉시구매_중_version_충돌이_나도_409_BID_CONFLICT() throws Exception {
		given(bidService.buyNow(anyLong(), anyLong()))
			.willThrow(new ObjectOptimisticLockingFailureException(Auction.class, 10L));

		mockMvc.perform(post("/api/auctions/10/buy-now"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("BID_CONFLICT"));
	}
}
