package com.team5.auction.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team5.auction.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
