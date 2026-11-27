package com.team5.auction.auction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.auction.dto.AuctionListResponse;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

	//경매 목록 조회
	@Query(value = """
		select new com.team5.auction.auction.dto.AuctionListResponse(
			a.id,
			a.title,
			(select i.url from AuctionImage i
			  where i.auction = a
			    and i.sortOrder = (select min(i2.sortOrder) from AuctionImage i2 where i2.auction = a)),
			c.name,
			a.currentPrice,
			a.buyNowPrice,
			a.bidCount,
			a.status,
			a.endAt)
		from Auction a
		join a.category c
		where a.status = :status
		  and c.id = :categoryId
		""",
		countQuery = """
		select count(a)
		from Auction a
		where a.status = :status
		  and (:categoryId is null or a.category.id = :categoryId)
		""")
	Page<AuctionListResponse> findAuctionList(
		@Param("categoryId") Long categoryId,
		@Param("status") AuctionStatus status,
		Pageable pageable);
}
