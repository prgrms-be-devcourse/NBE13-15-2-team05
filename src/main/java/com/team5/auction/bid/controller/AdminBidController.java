package com.team5.auction.bid.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.team5.auction.bid.dto.AdminBidResponse;
import com.team5.auction.bid.service.BidService;
import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.common.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "운영자 - 입찰")
@RestController
@RequiredArgsConstructor
public class AdminBidController {

	private final BidService bidService;

	@Operation(summary = "실제 입찰자 포함 입찰 내역")
	@GetMapping("/api/admin/auctions/{auctionId}/bids")
	public ApiResponse<PageResponse<AdminBidResponse>> getBids(
		@PathVariable Long auctionId,
		@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return ApiResponse.ok(PageResponse.from(bidService.getBidsForAdmin(auctionId, pageable)));
	}
}