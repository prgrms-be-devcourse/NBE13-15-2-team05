package com.team5.auction.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team5.auction.auction.domain.Auction;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
