package com.team5.auction.trade.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.common.entity.Address;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.point.service.Fee;
import com.team5.auction.point.service.FeeCalculator;
import com.team5.auction.point.service.PointService;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeRole;
import com.team5.auction.trade.domain.TradeStatus;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.trade.dto.TradeDetailResponse;
import com.team5.auction.trade.dto.TradeResponse;
import com.team5.auction.trade.event.TradeCreatedEvent;
import com.team5.auction.trade.event.TradeShippedEvent;
import com.team5.auction.trade.exception.TradeErrorCode;
import com.team5.auction.trade.repository.TradeRepository;
import com.team5.auction.user.domain.User;

/** TradeServiceImpl 단위 테스트. DB 없이 Mockito로 돕니다. */
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 10, 0);
	private static final int SHIP_DEADLINE_DAYS = 3;
	private static final int AUTO_CONFIRM_DAYS = 7;

	@Mock FeeCalculator feeCalculator;
	@Mock PointService pointService;
	@Mock TradeRepository tradeRepository;
	@Mock ApplicationEventPublisher eventPublisher;
	@Mock Auction auction;
	@Mock User buyer;
	@Mock User seller;

	TradeServiceImpl tradeService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
		AuctionPolicyProperties policy = new AuctionPolicyProperties(
			null, null, null,
			new AuctionPolicyProperties.TradePolicy(SHIP_DEADLINE_DAYS, AUTO_CONFIRM_DAYS),
			null);
		tradeService = new TradeServiceImpl(feeCalculator, clock, tradeRepository, eventPublisher, policy, pointService);
	}

	@Test
	void createTrade_거래를_PAID로_생성하고_이벤트를_발행한다() {
		// given
		Address buyerAddress = new Address("홍길동", "010-1234-5678", "12345", "서울시", "101호");
		Fee fee = new Fee(new BigDecimal("0.05"), 2_500L, 47_500L);

		given(buyer.hasAddress()).willReturn(true);
		given(buyer.getAddress()).willReturn(buyerAddress);
		given(buyer.getId()).willReturn(1L);
		given(seller.getId()).willReturn(2L);
		given(auction.getId()).willReturn(10L);
		given(auction.getTitle()).willReturn("테스트 경매");
		given(auction.getSeller()).willReturn(seller);
		given(feeCalculator.calculate(50_000L)).willReturn(fee);
		given(tradeRepository.save(any(Trade.class))).willAnswer(inv -> {
			Trade saved = inv.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 100L); // DB가 id를 채운 것처럼
			return saved;
		});

		// when
		Trade trade = tradeService.createTrade(auction, buyer, TradeType.BID_WIN, 50_000L);

		// then: 거래 필드
		assertThat(trade.getStatus()).isEqualTo(TradeStatus.PAID);
		assertThat(trade.getBuyer()).isSameAs(buyer);
		assertThat(trade.getSeller()).isSameAs(seller);
		assertThat(trade.getType()).isEqualTo(TradeType.BID_WIN);
		assertThat(trade.getPrice()).isEqualTo(50_000L);

		// 수수료 스냅샷 (R-54)
		assertThat(trade.getFeeRate()).isEqualByComparingTo("0.05");
		assertThat(trade.getFeeAmount()).isEqualTo(2_500L);
		assertThat(trade.getSettleAmount()).isEqualTo(47_500L);

		// 배송지는 복사본 (R-57)
		assertThat(trade.getShippingAddress()).isNotSameAs(buyerAddress);
		assertThat(trade.getShippingAddress()).usingRecursiveComparison().isEqualTo(buyerAddress);

		// 시각 (R-56)
		assertThat(trade.getTradedAt()).isEqualTo(NOW);
		assertThat(trade.getShipDeadlineAt()).isEqualTo(NOW.plusDays(SHIP_DEADLINE_DAYS));

		// then: 이벤트
		ArgumentCaptor<TradeCreatedEvent> captor = ArgumentCaptor.forClass(TradeCreatedEvent.class);
		then(eventPublisher).should().publishEvent(captor.capture());
		assertThat(captor.getValue()).isEqualTo(new TradeCreatedEvent(
			100L, 10L, "테스트 경매", 1L, 2L, TradeType.BID_WIN, 50_000L));
	}

	@Test
	void createTrade_구매자_배송지가_없으면_예외가_나고_저장하지_않는다() {
		// given
		given(buyer.hasAddress()).willReturn(false);
		given(buyer.getId()).willReturn(1L);

		// when & then
		assertThatThrownBy(() -> tradeService.createTrade(auction, buyer, TradeType.BUY_NOW, 50_000L))
			.isInstanceOf(IllegalStateException.class);

		then(tradeRepository).should(never()).save(any());
		then(eventPublisher).shouldHaveNoInteractions();
	}

	// ---- getTrades: 역할·상태 조합에 맞는 조회 메서드를 부르는지 ----

	private final Pageable pageable = PageRequest.of(0, 20);

	@Test
	void getTrades_구매자_상태없음이면_findByBuyerId() {
		given(tradeRepository.findByBuyerId(1L, pageable)).willReturn(Page.empty());

		tradeService.getTrades(1L, TradeRole.BUYER, null, pageable);

		then(tradeRepository).should().findByBuyerId(1L, pageable);
		then(tradeRepository).shouldHaveNoMoreInteractions();
	}

	@Test
	void getTrades_구매자_상태있음이면_findByBuyerIdAndStatus() {
		given(tradeRepository.findByBuyerIdAndStatus(1L, TradeStatus.PAID, pageable)).willReturn(Page.empty());

		tradeService.getTrades(1L, TradeRole.BUYER, TradeStatus.PAID, pageable);

		then(tradeRepository).should().findByBuyerIdAndStatus(1L, TradeStatus.PAID, pageable);
		then(tradeRepository).shouldHaveNoMoreInteractions();
	}

	@Test
	void getTrades_판매자_상태없음이면_findBySellerId() {
		given(tradeRepository.findBySellerId(2L, pageable)).willReturn(Page.empty());

		tradeService.getTrades(2L, TradeRole.SELLER, null, pageable);

		then(tradeRepository).should().findBySellerId(2L, pageable);
		then(tradeRepository).shouldHaveNoMoreInteractions();
	}

	@Test
	void getTrades_판매자_상태있음이면_findBySellerIdAndStatus() {
		given(tradeRepository.findBySellerIdAndStatus(2L, TradeStatus.SHIPPED, pageable)).willReturn(Page.empty());

		tradeService.getTrades(2L, TradeRole.SELLER, TradeStatus.SHIPPED, pageable);

		then(tradeRepository).should().findBySellerIdAndStatus(2L, TradeStatus.SHIPPED, pageable);
		then(tradeRepository).shouldHaveNoMoreInteractions();
	}

	@Test
	void getTrades_조회한_거래를_TradeResponse로_변환한다() {
		// given
		Trade trade = mock(Trade.class);
		given(auction.getId()).willReturn(10L);
		given(auction.getTitle()).willReturn("테스트 경매");
		given(trade.getId()).willReturn(100L);
		given(trade.getAuction()).willReturn(auction);
		given(trade.getType()).willReturn(TradeType.BUY_NOW);
		given(trade.getPrice()).willReturn(50_000L);
		given(trade.getStatus()).willReturn(TradeStatus.PAID);
		given(trade.getTradedAt()).willReturn(NOW);
		given(trade.getShipDeadlineAt()).willReturn(NOW.plusDays(SHIP_DEADLINE_DAYS));
		given(tradeRepository.findByBuyerId(1L, pageable))
			.willReturn(new PageImpl<>(List.of(trade), pageable, 1));

		// when
		Page<TradeResponse> result = tradeService.getTrades(1L, TradeRole.BUYER, null, pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0)).isEqualTo(new TradeResponse(
			100L, 10L, "테스트 경매", TradeType.BUY_NOW, 50_000L, TradeStatus.PAID,
			NOW, NOW.plusDays(SHIP_DEADLINE_DAYS), null));
	}

	// ---- getTrade: 당사자만 조회, 역할에 따라 상대방·금액 노출이 다른지 ----

	/** 구매자(id=1), 판매자(id=2)가 있는 PAID 상태 거래. */
	private Trade participantTrade() {
		given(auction.getSeller()).willReturn(seller);
		// 구매자·판매자 중 한쪽만 확인하는 경우가 있어 lenient
		lenient().when(buyer.getId()).thenReturn(1L);
		lenient().when(seller.getId()).thenReturn(2L);
		Address address = new Address("홍길동", "010-1234-5678", "12345", "서울시", "101호");
		Fee fee = new Fee(new BigDecimal("0.05"), 2_500L, 47_500L);
		Trade trade = Trade.create(auction, buyer, TradeType.BID_WIN, 50_000L, fee, address, NOW, SHIP_DEADLINE_DAYS);
		ReflectionTestUtils.setField(trade, "id", 100L);
		given(tradeRepository.findById(100L)).willReturn(Optional.of(trade));
		return trade;
	}

	@Test
	void getTrade_판매자가_조회하면_구매자_연락처와_수수료_정산금을_준다() {
		// given
		participantTrade();
		given(buyer.getNickname()).willReturn("구매자");
		given(buyer.getPhone()).willReturn("010-1111-1111");

		// when
		TradeDetailResponse result = tradeService.getTrade(2L, 100L);

		// then: 상대방은 구매자 (R-50)
		assertThat(result.tradeId()).isEqualTo(100L);
		assertThat(result.counterpart())
			.isEqualTo(new TradeDetailResponse.Counterpart("구매자", "010-1111-1111"));
		// 수수료·정산금은 판매자에게 노출 (R-54)
		assertThat(result.feeAmount()).isEqualTo(2_500L);
		assertThat(result.settleAmount()).isEqualTo(47_500L);
	}

	@Test
	void getTrade_구매자가_조회하면_판매자_연락처를_주고_수수료_정산금은_숨긴다() {
		// given
		participantTrade();
		given(seller.getNickname()).willReturn("판매자");
		given(seller.getPhone()).willReturn("010-2222-2222");

		// when
		TradeDetailResponse result = tradeService.getTrade(1L, 100L);

		// then: 상대방은 판매자 (R-50)
		assertThat(result.counterpart())
			.isEqualTo(new TradeDetailResponse.Counterpart("판매자", "010-2222-2222"));
		// 구매자에게는 수수료·정산금 숨김 (R-54)
		assertThat(result.feeAmount()).isNull();
		assertThat(result.settleAmount()).isNull();
	}

	@Test
	void getTrade_당사자가_아니면_TRADE_NOT_PARTICIPANT() {
		participantTrade();

		assertThatThrownBy(() -> tradeService.getTrade(3L, 100L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(TradeErrorCode.TRADE_NOT_PARTICIPANT);
	}

	@Test
	void getTrade_거래가_없으면_TRADE_NOT_FOUND() {
		given(tradeRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> tradeService.getTrade(1L, 999L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(TradeErrorCode.TRADE_NOT_FOUND);
	}

	// ---- registerTracking: 판매자만, PAID일 때만 송장 입력 ----

	@Test
	void registerTracking_판매자가_입력하면_SHIPPED로_바뀌고_이벤트를_발행한다() {
		// given
		Trade trade = participantTrade();
		given(auction.getTitle()).willReturn("테스트 경매");

		// when
		TradeDetailResponse result = tradeService.registerTracking(2L, 100L, "1234567890");

		// then: 상태·시각 (R-51, R-52)
		assertThat(trade.getStatus()).isEqualTo(TradeStatus.SHIPPED);
		assertThat(trade.getTrackingNumber()).isEqualTo("1234567890");
		assertThat(trade.getShippedAt()).isEqualTo(NOW);
		assertThat(trade.getAutoConfirmAt()).isEqualTo(NOW.plusDays(AUTO_CONFIRM_DAYS));
		// 판매자 기준 응답
		assertThat(result.feeAmount()).isEqualTo(2_500L);

		// then: 구매자에게 배송 알림 이벤트
		then(eventPublisher).should().publishEvent(
			new TradeShippedEvent(100L, "테스트 경매", 1L, "1234567890"));
	}

	@Test
	void registerTracking_구매자가_입력하면_TRADE_NOT_PARTICIPANT() {
		Trade trade = participantTrade();

		assertThatThrownBy(() -> tradeService.registerTracking(1L, 100L, "1234567890"))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(TradeErrorCode.TRADE_NOT_PARTICIPANT);

		assertThat(trade.getStatus()).isEqualTo(TradeStatus.PAID);
		then(eventPublisher).shouldHaveNoInteractions();
	}

	@Test
	void registerTracking_이미_SHIPPED면_TRADE_INVALID_STATUS() {
		Trade trade = participantTrade();
		ReflectionTestUtils.setField(trade, "status", TradeStatus.SHIPPED);

		assertThatThrownBy(() -> tradeService.registerTracking(2L, 100L, "1234567890"))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(TradeErrorCode.TRADE_INVALID_STATUS);

		then(eventPublisher).shouldHaveNoInteractions();
	}

	@Test
	void registerTracking_거래가_없으면_TRADE_NOT_FOUND() {
		given(tradeRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> tradeService.registerTracking(2L, 999L, "1234567890"))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(TradeErrorCode.TRADE_NOT_FOUND);
	}

	// ---- confirmTrade: 구매자만, SHIPPED일 때만 구매확정 ----

	@Test
	void confirmTrade_구매자가_확정하면_CONFIRMED로_바뀌고_포인트를_정산한다() {
		// given
		Trade trade = participantTrade();
		ReflectionTestUtils.setField(trade, "status", TradeStatus.SHIPPED);

		// when
		TradeDetailResponse result = tradeService.confirmTrade(1L, 100L);

		// then: 상태·시각
		assertThat(trade.getStatus()).isEqualTo(TradeStatus.CONFIRMED);
		assertThat(trade.getConfirmedAt()).isEqualTo(NOW);
		// 구매자 포인트 차감, 판매자 정산 (R-53)
		then(pointService).should().confirmPurchase(100L, 1L, 50_000L, 2L, 47_500L);
		// 구매자 기준 응답이라 수수료·정산금 숨김
		assertThat(result.feeAmount()).isNull();
	}

	@Test
	void confirmTrade_판매자가_확정하면_TRADE_NOT_PARTICIPANT() {
		Trade trade = participantTrade();
		ReflectionTestUtils.setField(trade, "status", TradeStatus.SHIPPED);

		assertThatThrownBy(() -> tradeService.confirmTrade(2L, 100L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(TradeErrorCode.TRADE_NOT_PARTICIPANT);

		assertThat(trade.getStatus()).isEqualTo(TradeStatus.SHIPPED);
		then(pointService).shouldHaveNoInteractions();
	}

	@Test
	void confirmTrade_SHIPPED가_아니면_TRADE_INVALID_STATUS() {
		participantTrade(); // PAID 상태

		assertThatThrownBy(() -> tradeService.confirmTrade(1L, 100L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(TradeErrorCode.TRADE_INVALID_STATUS);

		then(pointService).shouldHaveNoInteractions();
	}

	@Test
	void confirmTrade_거래가_없으면_TRADE_NOT_FOUND() {
		given(tradeRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> tradeService.confirmTrade(1L, 999L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(TradeErrorCode.TRADE_NOT_FOUND);
	}
}
