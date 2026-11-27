package com.team5.auction.auction.controller;


import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.auction.dto.AuctionCreateRequest;
import com.team5.auction.auction.dto.AuctionCreateResponse;
import com.team5.auction.auction.dto.AuctionListResponse;
import com.team5.auction.auction.dto.AuctionResponse;
import com.team5.auction.auction.dto.AuctionUpdateRequest;
import com.team5.auction.auction.service.AuctionService;
import com.team5.auction.common.response.ApiResponse;
import com.team5.auction.common.response.PageResponse;
import com.team5.auction.common.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping("/auctions")
    public ApiResponse<PageResponse<AuctionListResponse>> getAuctions(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "ACTIVE") AuctionStatus status,
            @PageableDefault(size = 20, sort = "endAt", direction = Sort.Direction.ASC) Pageable pageable) {

        return ApiResponse.ok(PageResponse.from(auctionService.getAuctions(categoryId, status, pageable)));
    }

    // ===== 아래는 틀만 잡아 둔 API (TODO) =====

    // 경매 상세 조회 - 전체(비로그인 가능)
    // 응답: 상세 ② { id, title, description, category{id,name}, sellerNickname, imageUrls[], startPrice, currentPrice,
    //               buyNowPrice, minimumNextBid, bidCount, status, startAt, endAt, extendCount, mine }
    // 에러: AUCTION_NOT_FOUND
    @GetMapping("/auctions/{auctionId}")
    public ApiResponse<AuctionResponse> getAuction(
            @AuthenticationPrincipal LoginUser loginUser,   // 비로그인이면 null → mine = false
            @PathVariable Long auctionId) {

        // TODO: auctionService.getAuction(auctionId, viewerId) 구현 후 연결
        throw new UnsupportedOperationException("경매 상세 조회는 아직 구현되지 않았습니다.");
    }

    // 경매 수정 - 판매자만
    // 요청: 등록과 같은 필드(durationDays 제외), 보낸 것만 수정 / 응답: 상세 ②
    // 에러: AUCTION_NOT_OWNER, AUCTION_NOT_EDITABLE + 등록 검증
    @PatchMapping("/auctions/{auctionId}")
    public ApiResponse<AuctionResponse> update(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long auctionId,
            @Valid @RequestBody AuctionUpdateRequest request) {

        // TODO: auctionService.update(loginUser.id(), auctionId, request) 구현 후 연결
        throw new UnsupportedOperationException("경매 수정은 아직 구현되지 않았습니다.");
    }

    // 경매 취소 - 판매자만 (입찰 0건일 때만, R-06)
    // 에러: AUCTION_NOT_OWNER, AUCTION_NOT_EDITABLE / 취소 시 AuctionCanceledEvent(byAdmin = false)
    @PostMapping("/auctions/{auctionId}/cancel")
    public ApiResponse<Void> cancel(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long auctionId) {

        // TODO: auctionService.cancel(loginUser.id(), auctionId) 구현 후 연결
        throw new UnsupportedOperationException("경매 취소는 아직 구현되지 않았습니다.");
    }

    // 내 판매 목록 - USER
    // 요청: status?, page, size / 응답: Page<목록 항목 ①> (AuctionListResponse 재사용)
    @GetMapping("/users/me/auctions")
    public ApiResponse<PageResponse<AuctionListResponse>> getMyAuctions(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) AuctionStatus status,   // null이면 전체 상태
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // TODO: auctionService.getMyAuctions(loginUser.id(), status, pageable) 구현 후 연결
        throw new UnsupportedOperationException("내 판매 목록은 아직 구현되지 않았습니다.");
    }
}
