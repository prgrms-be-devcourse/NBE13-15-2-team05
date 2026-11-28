package com.team5.auction.notification.event;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.team5.auction.TestcontainersConfiguration;
import com.team5.auction.bid.event.OutbidEvent;
import com.team5.auction.common.entity.RefType;
import com.team5.auction.notification.domain.Notification;
import com.team5.auction.notification.domain.NotificationType;
import com.team5.auction.notification.repository.NotificationRepository;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.repository.UserRepository;

/** 실제 트랜잭션으로 확인: 커밋되면 저장, 롤백되면 저장 안 함, 저장이 실패해도 원래 요청은 성공 (계약서 6.2) */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationEventListenerCommitTest {

	@Autowired
	private ApplicationEventPublisher eventPublisher;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	private NotificationRepository notificationRepository;
	@Autowired
	private UserRepository userRepository;

	private User user;

	@BeforeEach
	void setUp() {
		String key = UUID.randomUUID().toString().substring(0, 8);
		user = userRepository.save(User.createUser(key + "@test.com", "{noop}pw", "알림" + key, "010-0000-0000"));
	}

	@AfterEach
	void tearDown() {
		notificationRepository.deleteAll(notificationsOf(user));
		userRepository.delete(user);
	}

	@Test
	void 원래_트랜잭션이_커밋되면_알림이_저장된다() {
		transactionTemplate.executeWithoutResult(status ->
			eventPublisher.publishEvent(new OutbidEvent(10L, "맥북 프로", user.getId(), 15_000)));

		assertThat(notificationsOf(user))
			.singleElement()
			.satisfies(n -> {
				assertThat(n.getType()).isEqualTo(NotificationType.OUTBID);
				assertThat(n.getRefType()).isEqualTo(RefType.AUCTION);
				assertThat(n.getRefId()).isEqualTo(10L);
				assertThat(n.getReadAt()).isNull();
			});
	}

	@Test
	void 원래_트랜잭션이_롤백되면_알림이_저장되지_않는다() {
		transactionTemplate.executeWithoutResult(status -> {
			eventPublisher.publishEvent(new OutbidEvent(10L, "맥북 프로", user.getId(), 15_000));
			status.setRollbackOnly();
		});

		assertThat(notificationsOf(user)).isEmpty();
	}

	@Test
	void 알림_저장이_실패해도_원래_요청은_예외_없이_끝난다() {
		Long missingUserId = Long.MAX_VALUE; // 외래 키 위반으로 저장이 실패한다

		assertThatCode(() -> transactionTemplate.executeWithoutResult(status ->
			eventPublisher.publishEvent(new OutbidEvent(10L, "맥북 프로", missingUserId, 15_000))))
			.doesNotThrowAnyException();
	}

	private List<Notification> notificationsOf(User target) {
		return notificationRepository.findAll().stream()
			.filter(n -> n.getUser().getId().equals(target.getId()))
			.toList();
	}
}
