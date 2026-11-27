package com.team5.auction.auction.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team5.auction.auction.service.AuctionService;
import com.team5.auction.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

// /api/admin/** 는 SecurityConfig에서 ADMIN만 접근 가능
@RestController
@RequiredArgsConstructor
public class AdminAuctionController {

	private final AuctionService auctionService;

	// 운영자 강제 취소 - ADMIN (입찰이 있어도 취소 가능)
	// 에러: AUCTION_NOT_FOUND, AUCTION_NOT_EDITABLE(진행 중이 아님)
	// 최고 입찰자가 있으면 pointService.unlockBid(...), AuctionCanceledEvent(byAdmin = true) 발행
	@PostMapping("/api/admin/auctions/{auctionId}/cancel")
	public ApiResponse<Void> cancel(@PathVariable Long auctionId) {

		// TODO: auctionService.cancelByAdmin(auctionId) 구현 후 연결
		throw new UnsupportedOperationException("운영자 강제 취소는 아직 구현되지 않았습니다.");
	}
}
