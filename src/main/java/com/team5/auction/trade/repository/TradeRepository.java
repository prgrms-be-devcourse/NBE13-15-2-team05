package com.team5.auction.trade.repository;

import com.team5.auction.trade.domain.TradeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.team5.auction.trade.domain.Trade;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    @EntityGraph(attributePaths = "auction")
    Page<Trade> findByBuyerId(Long buyerId, Pageable pageable);

    @EntityGraph(attributePaths = "auction")
    Page<Trade> findByBuyerIdAndStatus(Long buyerId, TradeStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "auction")
    Page<Trade> findBySellerId(Long sellerId, Pageable pageable);

    @EntityGraph(attributePaths = "auction")
    Page<Trade> findBySellerIdAndStatus(Long sellerId, TradeStatus status, Pageable pageable);


}
