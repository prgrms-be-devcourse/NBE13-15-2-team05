package com.team5.auction.bid.dto;

import java.time.LocalDateTime;

import com.team5.auction.bid.domain.Bid;
import com.team5.auction.user.domain.User;

/** 운영자용 입찰 내역. 실제 입찰자를 포함합니다(R-18). */
public record AdminBidResponse(
	Long bidId,
	long amount,
	LocalDateTime bidAt,
	Bidder bidder
) {

	/** 입찰자를 함께 불러온 Bid로 만든다 (findWithBidderByAuctionId) */
	public static AdminBidResponse from(Bid bid) {
		return new AdminBidResponse(bid.getId(), bid.getAmount(), bid.getBidAt(), Bidder.from(bid.getBidder()));
	}

	public record Bidder(Long id, String nickname, String email) {

		public static Bidder from(User user) {
			return new Bidder(user.getId(), user.getNickname(), user.getEmail());
		}
	}
}