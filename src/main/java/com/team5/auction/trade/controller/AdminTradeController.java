package com.team5.auction.trade.controller;

import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "운영자 - 거래")
@RestController
@RequestMapping("/api/admin/trades")
@RequiredArgsConstructor
public class AdminTradeController {

    private final TradeService tradeService;

    @Operation(summary = "거래 강제 취소", description = "관리자 전용. CONFIRMED, CANCELED 상태는 취소 불가")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelTrade (
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id
            ) {
        tradeService.cancelTradeById(user.id(), id);
        return ApiResponse.ok();
    }
}
