package com.team5.auction.notification.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.team5.auction.bid.event.OutbidEvent;

/**
 * TODO(알림): 이벤트 7종을 받아 Notification 저장. (계약서 6장)
 * - AFTER_COMMIT 시점에는 원래 트랜잭션이 끝났으므로 REQUIRES_NEW가 꼭 필요합니다.
 * - 저장 실패는 로그만 남기고 원래 입찰·거래에는 영향을 주지 않습니다.
 * 아래는 형식 예시입니다. 나머지 이벤트도 같은 형태로 추가하세요.
 */
@Component
public class NotificationEventListener {

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handle(OutbidEvent event) {
		// TODO(알림)
	}
}
