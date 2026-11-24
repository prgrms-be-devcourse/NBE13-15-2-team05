package com.team5.auction.point.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team5.auction.point.domain.BalanceHistory;

public interface BalanceHistoryRepository extends JpaRepository<BalanceHistory, Long> {
}
