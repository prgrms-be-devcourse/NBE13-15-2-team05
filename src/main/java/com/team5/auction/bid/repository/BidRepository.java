package com.team5.auction.bid.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.team5.auction.bid.domain.Bid;

public interface BidRepository extends JpaRepository<Bid, Long> {

	/** 경매의 입찰 내역 (공개용). 입찰자는 id 비교에만 쓰므로 함께 불러오지 않는다. */
	Page<Bid> findByAuctionId(Long auctionId, Pageable pageable);

	/** 경매의 입찰 내역 (운영자용). 입찰자 정보를 함께 불러온다. */
	@EntityGraph(attributePaths = "bidder")
	Page<Bid> findWithBidderByAuctionId(Long auctionId, Pageable pageable);

	/** 내 입찰 목록. 경매 제목·상태·현재가를 함께 불러온다. */
	@EntityGraph(attributePaths = "auction")
	Page<Bid> findWithAuctionByBidderId(Long bidderId, Pageable pageable);
}
