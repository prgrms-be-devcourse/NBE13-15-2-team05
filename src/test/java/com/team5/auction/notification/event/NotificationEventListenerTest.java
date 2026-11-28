package com.team5.auction.notification.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

/** 이벤트 7종 → 받는 사람, 알림 타입, 문구 (계약서 6.1 표) */
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

	private static final String TITLE = "맥북 프로";

	@Mock
	private NotificationService notificationService;
	@InjectMocks
	private NotificationEventListener listener;

	@Test
	void 상위_입찰이_들어오면_이전_최고_입찰자에게_OUTBID() {
		listener.handle(new OutbidEvent(10L, TITLE, 1L, 15_000));

		then(notificationService).should().send(eq(1L), eq(NotificationType.OUTBID),
			eq("'맥북 프로' 경매에 더 높은 입찰(15,000원)이 들어왔습니다. 묶여 있던 포인트는 풀렸습니다."),
			eq(RefType.AUCTION), eq(10L));
	}

	@Test
	void 낙찰되면_구매자에게_AUCTION_WON_판매자에게_AUCTION_SOLD() {
		listener.handle(new TradeCreatedEvent(100L, 10L, TITLE, 1L, 2L, TradeType.BID_WIN, 50_000));

		then(notificationService).should().send(eq(1L), eq(NotificationType.AUCTION_WON),
			eq("'맥북 프로' 경매를 50,000원에 낙찰받았습니다."), eq(RefType.TRADE), eq(100L));
		then(notificationService).should().send(eq(2L), eq(NotificationType.AUCTION_SOLD),
			eq("'맥북 프로' 경매가 50,000원에 판매되었습니다. 송장을 입력해 주세요."), eq(RefType.TRADE), eq(100L));
	}

	@Test
	void 즉시구매면_구매자_문구가_즉시구매다() {
		listener.handle(new TradeCreatedEvent(100L, 10L, TITLE, 1L, 2L, TradeType.BUY_NOW, 90_000));

		then(notificationService).should().send(eq(1L), eq(NotificationType.AUCTION_WON),
			eq("'맥북 프로' 경매를 90,000원에 즉시구매했습니다."), eq(RefType.TRADE), eq(100L));
	}

	@Test
	void 유찰되면_판매자에게_AUCTION_FAILED() {
		listener.handle(new AuctionFailedEvent(10L, TITLE, 2L));

		then(notificationService).should().send(eq(2L), eq(NotificationType.AUCTION_FAILED),
			anyString(), eq(RefType.AUCTION), eq(10L));
	}

	@Test
	void 운영자가_취소하면_판매자와_최고_입찰자에게_AUCTION_CANCELED() {
		listener.handle(new AuctionCanceledEvent(10L, TITLE, 2L, 1L, true));

		then(notificationService).should().send(eq(2L), eq(NotificationType.AUCTION_CANCELED),
			eq("'맥북 프로' 경매가 운영자에 의해 취소되었습니다."), eq(RefType.AUCTION), eq(10L));
		then(notificationService).should().send(eq(1L), eq(NotificationType.AUCTION_CANCELED),
			eq("'맥북 프로' 경매가 운영자에 의해 취소되었습니다. 묶여 있던 포인트는 풀렸습니다."),
			eq(RefType.AUCTION), eq(10L));
	}

	@Test
	void 운영자가_취소했는데_입찰자가_없으면_판매자에게만() {
		listener.handle(new AuctionCanceledEvent(10L, TITLE, 2L, null, true));

		then(notificationService).should().send(eq(2L), any(), anyString(), any(), any());
		then(notificationService).shouldHaveNoMoreInteractions();
	}

	@Test
	void 판매자_본인_취소는_알림이_없다() {
		listener.handle(new AuctionCanceledEvent(10L, TITLE, 2L, null, false));

		then(notificationService).shouldHaveNoInteractions();
	}

	@Test
	void 송장을_입력하면_구매자에게_SHIPPED() {
		listener.handle(new TradeShippedEvent(100L, TITLE, 1L, "1234-5678"));

		then(notificationService).should().send(eq(1L), eq(NotificationType.SHIPPED),
			eq("'맥북 프로' 상품이 발송되었습니다. 송장번호: 1234-5678"), eq(RefType.TRADE), eq(100L));
	}

	@Test
	void 자동_확정_전에_구매자에게_CONFIRM_SOON() {
		listener.handle(new TradeConfirmSoonEvent(100L, TITLE, 1L, LocalDateTime.of(2026, 12, 3, 9, 5)));

		then(notificationService).should().send(eq(1L), eq(NotificationType.CONFIRM_SOON),
			eq("'맥북 프로' 거래가 12월 3일 09:05에 자동으로 구매확정됩니다. 문제가 있으면 그 전에 알려 주세요."),
			eq(RefType.TRADE), eq(100L));
	}

	@Test
	void 송장_기한으로_취소되면_구매자와_판매자에게_TRADE_CANCELED() {
		listener.handle(new TradeCanceledEvent(100L, TITLE, 1L, 2L, CancelReason.SHIP_TIMEOUT));

		then(notificationService).should().send(eq(1L), eq(NotificationType.TRADE_CANCELED),
			eq("'맥북 프로' 거래가 송장 입력 기한이 지나 취소되었습니다. 포인트는 환불되었습니다."),
			eq(RefType.TRADE), eq(100L));
		then(notificationService).should().send(eq(2L), eq(NotificationType.TRADE_CANCELED),
			eq("'맥북 프로' 거래가 송장 입력 기한이 지나 취소되었습니다."), eq(RefType.TRADE), eq(100L));
	}

	@Test
	void 운영자가_거래를_취소하면_사유가_운영자다() {
		listener.handle(new TradeCanceledEvent(100L, TITLE, 1L, 2L, CancelReason.ADMIN));

		then(notificationService).should().send(eq(2L), eq(NotificationType.TRADE_CANCELED),
			eq("'맥북 프로' 거래가 운영자에 의해 취소되었습니다."), eq(RefType.TRADE), eq(100L));
	}

	@Test
	void 한_명의_저장이_실패해도_예외를_던지지_않고_다른_사람_알림은_보낸다() {
		willThrow(new IllegalStateException("DB 오류"))
			.given(notificationService).send(eq(1L), any(), anyString(), any(), any());

		assertThatCode(() -> listener.handle(new TradeCreatedEvent(100L, 10L, TITLE, 1L, 2L, TradeType.BID_WIN, 50_000)))
			.doesNotThrowAnyException();
		then(notificationService).should().send(eq(2L), eq(NotificationType.AUCTION_SOLD), anyString(), any(), any());
	}
}
