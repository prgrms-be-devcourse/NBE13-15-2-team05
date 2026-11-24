package com.team5.auction.bid.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team5.auction.bid.domain.Bid;

public interface BidRepository extends JpaRepository<Bid, Long> {
}
