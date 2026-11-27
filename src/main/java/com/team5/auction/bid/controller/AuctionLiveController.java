package com.team5.auction.bid.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.team5.auction.bid.service.BidService;
import com.team5.auction.bid.stream.AuctionLiveStream;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "입찰")
@RestController
@RequiredArgsConstructor
public class AuctionLiveController {

	private final BidService bidService;
	private final AuctionLiveStream auctionLiveStream;

	@Operation(summary = "실시간 현재가 구독 (SSE)",
		description = "연결 직후 현재 상태를, 이후 입찰이 성공할 때마다 price 이벤트를 보냅니다. "
			+ "최고 입찰자는 포함하지 않습니다 (R-18). 로그인 없이도 구독 가능")
	@GetMapping(value = "/api/auctions/{auctionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@PathVariable Long auctionId) {
		return auctionLiveStream.subscribe(auctionId, bidService.getLiveState(auctionId));
	}
}
