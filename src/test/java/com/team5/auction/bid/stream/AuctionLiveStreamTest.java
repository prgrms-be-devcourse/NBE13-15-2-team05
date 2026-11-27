package com.team5.auction.bid.stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.auction.exception.AuctionErrorCode;
import com.team5.auction.bid.controller.AuctionLiveController;
import com.team5.auction.bid.dto.AuctionLiveResponse;
import com.team5.auction.bid.event.BidPlacedEvent;
import com.team5.auction.bid.service.BidService;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.exception.GlobalExceptionHandler;

/** 실시간 현재가 구독(SSE): 연결 직후 현재 상태, 입찰 커밋 후 전송, 연결 정리 */
@ExtendWith(MockitoExtension.class)
class AuctionLiveStreamTest {

	private static final Long AUCTION_ID = 10L;
	private static final LocalDateTime END_AT = LocalDateTime.of(2026, 11, 27, 12, 0);

	@Mock
	private BidService bidService;

	private AuctionLiveStream stream;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		stream = new AuctionLiveStream();
		mockMvc = MockMvcBuilders.standaloneSetup(new AuctionLiveController(bidService, stream))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void 구독하면_현재_상태를_price_이벤트로_바로_보낸다() throws Exception {
		MvcResult result = subscribe(AUCTION_ID, 12_000);

		assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
		assertThat(body(result))
			.contains("event:price")
			.contains("\"currentPrice\":12000")
			.contains("\"minimumNextBid\":13000")
			.contains("\"status\":\"ACTIVE\"");
		assertThat(stream.subscriberCount(AUCTION_ID)).isEqualTo(1);
	}

	@Test
	void 입찰이_커밋되면_같은_경매_구독자에게_새_현재가를_보낸다() throws Exception {
		MvcResult result = subscribe(AUCTION_ID, 12_000);

		stream.onBidPlaced(new BidPlacedEvent(AUCTION_ID, 15_000, 16_000, 2, END_AT.plusHours(1), true, AuctionStatus.ACTIVE));

		assertThat(body(result))
			.contains("\"currentPrice\":15000")
			.contains("\"extended\":true");
	}

	@Test
	void 다른_경매의_입찰은_보내지_않는다() throws Exception {
		MvcResult result = subscribe(AUCTION_ID, 12_000);

		stream.onBidPlaced(new BidPlacedEvent(99L, 70_000, 71_000, 5, END_AT, false, AuctionStatus.ACTIVE));

		assertThat(body(result)).doesNotContain("70000");
	}

	@Test
	void 구독자가_없는_경매의_입찰은_조용히_넘어간다() {
		assertThatCode(() -> stream.onBidPlaced(
			new BidPlacedEvent(AUCTION_ID, 15_000, 16_000, 2, END_AT, false, AuctionStatus.ACTIVE)))
			.doesNotThrowAnyException();
	}

	@Test
	void 연결이_끝나면_구독_목록에서_빠진다() throws Exception {
		MvcResult result = subscribe(AUCTION_ID, 12_000);

		result.getRequest().getAsyncContext().complete();

		assertThat(stream.subscriberCount(AUCTION_ID)).isZero();
	}

	@Test
	void 하트비트는_빈_신호를_보낸다() throws Exception {
		MvcResult result = subscribe(AUCTION_ID, 12_000);

		stream.heartbeat();

		assertThat(body(result)).contains(":ping");
	}

	@Test
	void 없는_경매를_구독하면_404_AUCTION_NOT_FOUND() throws Exception {
		given(bidService.getLiveState(AUCTION_ID)).willThrow(new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

		mockMvc.perform(get("/api/auctions/{id}/stream", AUCTION_ID).accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.error.code").value("AUCTION_NOT_FOUND"));
		assertThat(stream.subscriberCount(AUCTION_ID)).isZero();
	}

	private MvcResult subscribe(Long auctionId, long currentPrice) throws Exception {
		given(bidService.getLiveState(auctionId)).willReturn(new AuctionLiveResponse(
			auctionId, currentPrice, currentPrice + 1_000, 1, END_AT, false, AuctionStatus.ACTIVE));
		return mockMvc.perform(get("/api/auctions/{id}/stream", auctionId).accept(MediaType.TEXT_EVENT_STREAM))
			.andExpect(request().asyncStarted())
			.andReturn();
	}

	private String body(MvcResult result) throws Exception {
		return result.getResponse().getContentAsString();
	}
}
