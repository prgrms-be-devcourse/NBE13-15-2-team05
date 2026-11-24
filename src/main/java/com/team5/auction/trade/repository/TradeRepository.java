package com.team5.auction.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team5.auction.trade.domain.Trade;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
