package com.team5.auction.trade.controller;

import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.common.response.PageResponse;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.trade.domain.TradeRole;
import com.team5.auction.trade.domain.TradeStatus;
import com.team5.auction.trade.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.awt.print.Pageable;
import java.util.Optional;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @GetMapping
    public ApiResponse<PageResponse<?>> getTrades(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam TradeRole role,
            @RequestParam(required = false) TradeStatus status,
            @PageableDefault(size=20) Pageable pageable
            ) {
        throw new UnsupportedOperationException("미구현");
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getTrades(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id
    ) {
        throw new UnsupportedOperationException("미구현");
    }

    @PatchMapping("/{id}/tracking")
    public ApiResponse<?> registerTracking (
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id,
            @Valid @RequestBody Optional<?> request
    ) {
        throw new UnsupportedOperationException("미구현");
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<?> confirmTrade (
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id
    ) {
        throw new UnsupportedOperationException("미구현");
    }

}
