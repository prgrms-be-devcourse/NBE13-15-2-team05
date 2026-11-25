package com.team5.auction.trade.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

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
import com.team5.auction.point.service.Fee;
import com.team5.auction.point.service.FeeCalculator;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeRole;
import com.team5.auction.trade.domain.TradeStatus;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.trade.dto.TradeResponse;
import com.team5.auction.trade.event.TradeCreatedEvent;
import com.team5.auction.trade.repository.TradeRepository;
import com.team5.auction.user.domain.User;

/** TradeServiceImpl 단위 테스트. DB 없이 Mockito로 돕니다. */
@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 10, 0);
	private static final int SHIP_DEADLINE_DAYS = 3;

	@Mock FeeCalculator feeCalculator;
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
			new AuctionPolicyProperties.TradePolicy(SHIP_DEADLINE_DAYS, 7),
			null);
		tradeService = new TradeServiceImpl(feeCalculator, clock, tradeRepository, eventPublisher, policy);
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
}
