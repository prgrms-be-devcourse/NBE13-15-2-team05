package com.team5.auction.trade.controller;

import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.trade.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/trades")
@RequiredArgsConstructor
public class AdminTradeController {

    private final TradeService tradeService;

    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancelTrade (
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id
            ) {
        throw new UnsupportedOperationException("미구현");
    }
}
