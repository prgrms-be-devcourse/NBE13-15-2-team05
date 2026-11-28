package com.team5.auction.notification.event;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.team5.auction.auction.event.AuctionCanceledEvent;
import com.team5.auction.auction.event.AuctionFailedEvent;
import com.team5.auction.bid.event.OutbidEvent;
import com.team5.auction.common.entity.RefType;
import com.team5.auction.notification.domain.NotificationType;
import com.team5.auction.notification.service.NotificationService;
import com.team5.auction.trade.domain.CancelReason;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.trade.event.TradeCanceledEvent;
import com.team5.auction.trade.event.TradeConfirmSoonEvent;
import com.team5.auction.trade.event.TradeCreatedEvent;
import com.team5.auction.trade.event.TradeShippedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 도메인 이벤트 7종을 받아 알림을 저장한다 (계약서 6장). 알림 문구는 여기서 만든다.
 *
 * <p>입찰·거래가 커밋된 뒤에만 받으므로 롤백된 입찰·거래에는 알림이 가지 않는다.
 * 저장은 NotificationService가 받는 사람마다 새 트랜잭션으로 한다.
 * 저장이 실패해도 이미 커밋된 입찰·거래에는 영향이 없다 (계약서 6.2의 3번).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

	private static final DateTimeFormatter CONFIRM_AT = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

	private final NotificationService notificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void handle(OutbidEvent e) {
		send(e.userId(), NotificationType.OUTBID, RefType.AUCTION, e.auctionId(),
			"'%s' 경매에 더 높은 입찰(%,d원)이 들어왔습니다. 묶여 있던 포인트는 풀렸습니다."
				.formatted(e.auctionTitle(), e.newPrice()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void handle(TradeCreatedEvent e) {
		String won = e.type() == TradeType.BUY_NOW
			? "'%s' 경매를 %,d원에 즉시구매했습니다."
			: "'%s' 경매를 %,d원에 낙찰받았습니다.";
		send(e.buyerId(), NotificationType.AUCTION_WON, RefType.TRADE, e.tradeId(),
			won.formatted(e.auctionTitle(), e.price()));
		send(e.sellerId(), NotificationType.AUCTION_SOLD, RefType.TRADE, e.tradeId(),
			"'%s' 경매가 %,d원에 판매되었습니다. 송장을 입력해 주세요.".formatted(e.auctionTitle(), e.price()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void handle(AuctionFailedEvent e) {
		send(e.sellerId(), NotificationType.AUCTION_FAILED, RefType.AUCTION, e.auctionId(),
			"'%s' 경매가 입찰 없이 종료되었습니다.".formatted(e.auctionTitle()));
	}

	/** 판매자 본인 취소는 알림을 보내지 않는다 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void handle(AuctionCanceledEvent e) {
		if (!e.byAdmin()) {
			return;
		}
		send(e.sellerId(), NotificationType.AUCTION_CANCELED, RefType.AUCTION, e.auctionId(),
			"'%s' 경매가 운영자에 의해 취소되었습니다.".formatted(e.auctionTitle()));
		if (e.highestBidderId() != null) {
			send(e.highestBidderId(), NotificationType.AUCTION_CANCELED, RefType.AUCTION, e.auctionId(),
				"'%s' 경매가 운영자에 의해 취소되었습니다. 묶여 있던 포인트는 풀렸습니다.".formatted(e.auctionTitle()));
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void handle(TradeShippedEvent e) {
		send(e.buyerId(), NotificationType.SHIPPED, RefType.TRADE, e.tradeId(),
			"'%s' 상품이 발송되었습니다. 송장번호: %s".formatted(e.auctionTitle(), e.trackingNumber()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void handle(TradeConfirmSoonEvent e) {
		send(e.buyerId(), NotificationType.CONFIRM_SOON, RefType.TRADE, e.tradeId(),
			"'%s' 거래가 %s에 자동으로 구매확정됩니다. 문제가 있으면 그 전에 알려 주세요."
				.formatted(e.auctionTitle(), e.autoConfirmAt().format(CONFIRM_AT)));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void handle(TradeCanceledEvent e) {
		String reason = e.reason() == CancelReason.SHIP_TIMEOUT ? "송장 입력 기한이 지나" : "운영자에 의해";
		send(e.buyerId(), NotificationType.TRADE_CANCELED, RefType.TRADE, e.tradeId(),
			"'%s' 거래가 %s 취소되었습니다. 포인트는 환불되었습니다.".formatted(e.auctionTitle(), reason));
		send(e.sellerId(), NotificationType.TRADE_CANCELED, RefType.TRADE, e.tradeId(),
			"'%s' 거래가 %s 취소되었습니다.".formatted(e.auctionTitle(), reason));
	}

	/**
	 * 받는 사람마다 따로 저장해서, 한 명이 실패해도 다른 사람 알림은 남긴다.
	 * 예외를 그대로 두면 스프링이 로그만 남기고 넘어가지만, 그 뒤의 받는 사람 저장까지 건너뛰게 된다
	 */
	private void send(Long userId, NotificationType type, RefType refType, Long refId, String message) {
		try {
			notificationService.send(userId, type, message, refType, refId);
		} catch (RuntimeException ex) {
			log.warn("알림 저장 실패. userId={}, type={}, refId={}", userId, type, refId, ex);
		}
	}
}
