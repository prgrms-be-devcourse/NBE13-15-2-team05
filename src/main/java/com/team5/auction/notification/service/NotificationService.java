package com.team5.auction.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.team5.auction.common.entity.RefType;
import com.team5.auction.notification.domain.Notification;
import com.team5.auction.notification.domain.NotificationType;
import com.team5.auction.notification.repository.NotificationRepository;
import com.team5.auction.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** 알림 서비스 (R-63) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	/**
	 * 알림 한 건 저장.
	 * 커밋 후 리스너에서 부르므로 원래 트랜잭션은 이미 끝났다. 새 트랜잭션을 열어야 저장된다 (계약서 6.2).
	 * 받는 사람은 이벤트에 id로만 오므로 조회 없이 참조만 건다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void send(Long userId, NotificationType type, String message, RefType refType, Long refId) {
		notificationRepository.save(
			Notification.create(userRepository.getReferenceById(userId), type, message, refType, refId));
	}
}
