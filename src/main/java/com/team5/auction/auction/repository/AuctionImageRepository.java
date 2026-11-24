package com.team5.auction.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team5.auction.auction.domain.AuctionImage;

public interface AuctionImageRepository extends JpaRepository<AuctionImage, Long> {
}
