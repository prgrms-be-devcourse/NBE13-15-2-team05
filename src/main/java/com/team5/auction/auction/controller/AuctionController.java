package com.team5.auction.auction.controller;


import com.team5.auction.auction.dto.AuctionCreateRequest;
import com.team5.auction.auction.dto.AuctionCreateResponse;
import com.team5.auction.auction.service.AuctionService;
import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.common.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;

    @PostMapping("/auctions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuctionCreateResponse> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody AuctionCreateRequest request) {

        return ApiResponse.ok(new AuctionCreateResponse(auctionService.create(loginUser.id(), request).getId()));
    }
}
