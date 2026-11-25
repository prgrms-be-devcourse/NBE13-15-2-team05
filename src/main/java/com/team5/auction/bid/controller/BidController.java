package com.team5.auction.bid.controller;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.team5.auction.bid.dto.BidHistoryResponse;
import com.team5.auction.bid.dto.BidRequest;
import com.team5.auction.bid.dto.BidResultResponse;
import com.team5.auction.bid.dto.MyBidResponse;
import com.team5.auction.bid.service.BidService;
import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.common.response.PageResponse;
import com.team5.auction.common.security.LoginUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "입찰")
@RestController
@RequiredArgsConstructor
public class BidController {

	private final BidService bidService;

	@Operation(summary = "입찰", description = "R-10 ~ R-19. 동시 입찰 충돌 시 409 BID_CONFLICT")
	@PostMapping("/api/auctions/{auctionId}/bids")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<BidResultResponse> placeBid(
		@PathVariable Long auctionId,
		@Valid @RequestBody BidRequest request,
		@AuthenticationPrincipal LoginUser loginUser
	) {
		return ApiResponse.ok(bidService.placeBid(auctionId, loginUser.id(), request.amount()));
	}

	@Operation(summary = "즉시구매", description = "즉시구매가로 입찰한 것과 같게 처리 (R-44)")
	@PostMapping("/api/auctions/{auctionId}/buy-now")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<BidResultResponse> buyNow(
		@PathVariable Long auctionId,
		@AuthenticationPrincipal LoginUser loginUser
	) {
		return ApiResponse.ok(bidService.buyNow(auctionId, loginUser.id()));
	}

	@Operation(summary = "경매의 입찰 내역", description = "금액과 시각만 공개 (R-18). 로그인 없이도 조회 가능")
	@GetMapping("/api/auctions/{auctionId}/bids")
	public ApiResponse<PageResponse<BidHistoryResponse>> getBids(
		@PathVariable Long auctionId,
		@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
		@AuthenticationPrincipal @Nullable LoginUser loginUser
	) {
		Long viewerId = loginUser == null ? null : loginUser.id();
		return ApiResponse.ok(PageResponse.from(bidService.getBids(auctionId, viewerId, pageable)));
	}

	@Operation(summary = "내 입찰 목록")
	@GetMapping("/api/users/me/bids")
	public ApiResponse<PageResponse<MyBidResponse>> getMyBids(
		@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
		@AuthenticationPrincipal LoginUser loginUser
	) {
		return ApiResponse.ok(PageResponse.from(bidService.getMyBids(loginUser.id(), pageable)));
	}
}