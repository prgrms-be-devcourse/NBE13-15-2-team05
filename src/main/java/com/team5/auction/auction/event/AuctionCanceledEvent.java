package com.team5.auction.auction.event;

import org.jspecify.annotations.Nullable;

/** 판매자 취소·운영자 강제 취소 시 발행. byAdmin이면 → AUCTION_CANCELED: 판매자, 최고 입찰자(있을 때). 판매자 본인 취소는 알림 없음 */
public record AuctionCanceledEvent(Long auctionId, String auctionTitle, Long sellerId, @Nullable Long highestBidderId, boolean byAdmin) {
}
