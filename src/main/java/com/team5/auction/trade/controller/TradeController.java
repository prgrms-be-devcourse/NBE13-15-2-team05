package com.team5.auction.trade.controller;

import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.common.response.PageResponse;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.trade.domain.TradeRole;
import com.team5.auction.trade.domain.TradeStatus;
import com.team5.auction.trade.dto.TrackingRequest;
import com.team5.auction.trade.dto.TradeDetailResponse;
import com.team5.auction.trade.dto.TradeResponse;
import com.team5.auction.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Tag(name = "거래")
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "내 거래 목록 조회", description = "role(buyer|seller)과 status로 필터링, 페이징")
    @GetMapping
    public ApiResponse<PageResponse<TradeResponse>> getTrades(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam TradeRole role,
            @RequestParam(required = false) TradeStatus status,
            @PageableDefault(size = 20, sort = "tradedAt", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        return ApiResponse.ok(PageResponse.from(tradeService.getTrades(user.id(), role, status, pageable)));
    }

    @Operation(summary = "거래 상세 조회", description = "거래 당사자만 조회 가능 (R-50)")
    @GetMapping("/{id}")
    public ApiResponse<TradeDetailResponse> getTrades(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(tradeService.getTrade(user.id(), id));
    }

    @Operation(summary = "운송장 등록", description = "판매자가 운송장 번호를 등록. PAID 상태에서만 가능")
    @PatchMapping("/{id}/tracking")
    public ApiResponse<TradeDetailResponse> registerTracking (
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id,
            @Valid @RequestBody TrackingRequest request
    ) {
        return ApiResponse.ok(tradeService.registerTracking(user.id(), id, request.trackingNumber()));
    }

    @Operation(summary = "구매 확정", description = "구매자가 수령 후 거래를 확정")
    @PostMapping("/{id}/confirm")
    public ApiResponse<TradeDetailResponse> confirmTrade (
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(tradeService.confirmPurchase(user.id(), id));
    }
}
