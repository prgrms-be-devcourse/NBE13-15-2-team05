package com.team5.auction.bid.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.auction.exception.AuctionErrorCode;
import com.team5.auction.auction.repository.AuctionRepository;
import com.team5.auction.bid.domain.Bid;
import com.team5.auction.bid.dto.BidResultResponse;
import com.team5.auction.bid.event.OutbidEvent;
import com.team5.auction.bid.exception.BidErrorCode;
import com.team5.auction.bid.repository.BidRepository;
import com.team5.auction.common.config.TimeConfig;
import com.team5.auction.common.entity.Address;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.exception.ErrorCode;
import com.team5.auction.point.service.PointService;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.trade.service.TradeService;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.repository.UserRepository;

/** placeBid 8단계 (기술서 9.1). 협력 서비스는 목으로 두고 호출 인자와 경매 상태를 확인한다. */
@ExtendWith(MockitoExtension.class)
class BidServicePlaceBidTest {

	private static final Long AUCTION_ID = 10L;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 26, 12, 0);
	private static final LocalDateTime END_AT = NOW.plusDays(1);

	@Mock
	private BidRepository bidRepository;
	@Mock
	private AuctionRepository auctionRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private PointService pointService;
	@Mock
	private TradeService tradeService;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	private BidService bidService;

	private final User seller = user(1L, true);
	private final User prevBidder = user(2L, true);
	private final User bidder = user(3L, true);

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW.atZone(TimeConfig.ZONE).toInstant(), TimeConfig.ZONE);
		BidUnitPolicy unitPolicy = currentPrice -> 1_000;
		bidService = new BidService(bidRepository, auctionRepository, userRepository, unitPolicy,
			pointService, tradeService, clock, eventPublisher);
	}

	@Test
	void 첫_입찰은_시작가_이상이면_성공하고_해제할_이전_입찰자가_없다() {
		Auction auction = auction(10_000, 0, null, null);
		givenAuctionAndBidder(auction, bidder);
		givenSaveAssignsId();

		BidResultResponse result = bidService.placeBid(AUCTION_ID, bidder.getId(), 10_000);

		then(pointService).should().lockForBid(AUCTION_ID, bidder.getId(), 10_000, null, 0);
		assertThat(auction.getCurrentPrice()).isEqualTo(10_000);
		assertThat(auction.getHighestBidder()).isSameAs(bidder);
		assertThat(result).isEqualTo(new BidResultResponse(500L, 10_000, 10_000, END_AT, false, false, null));
		then(eventPublisher).shouldHaveNoInteractions(); // 밀려난 사람이 없으니 OUTBID 알림도 없다
	}

	@Test
	void 이전_최고_입찰자가_있으면_같은_호출에서_그_금액을_해제한다() {
		Auction auction = auction(12_000, 1, prevBidder, null);
		givenAuctionAndBidder(auction, bidder);
		givenSaveAssignsId();

		bidService.placeBid(AUCTION_ID, bidder.getId(), 15_000);

		then(pointService).should().lockForBid(AUCTION_ID, bidder.getId(), 15_000, prevBidder.getId(), 12_000);
		then(bidRepository).should().save(argThat(bid -> bid.getAmount() == 15_000 && bid.getBidAt().equals(NOW)));
		then(eventPublisher).should().publishEvent(new OutbidEvent(AUCTION_ID, "빈티지 카메라", prevBidder.getId(), 15_000));
	}

	@Test
	void 마감_30분_안의_입찰이면_마감이_연장된다() {
		Auction auction = auction(12_000, 1, prevBidder, null);
		ReflectionTestUtils.setField(auction, "endAt", NOW.plusMinutes(10));
		givenAuctionAndBidder(auction, bidder);
		givenSaveAssignsId();

		BidResultResponse result = bidService.placeBid(AUCTION_ID, bidder.getId(), 13_000);

		assertThat(result.extended()).isTrue();
		assertThat(result.endAt()).isEqualTo(NOW.plusMinutes(60));
	}

	@Test
	void 즉시구매가_이상이면_즉시구매가로_묶고_낙찰_거래를_만든다() {
		Auction auction = auction(12_000, 1, prevBidder, 50_000L);
		givenAuctionAndBidder(auction, bidder);
		givenSaveAssignsId();
		Trade trade = mock(Trade.class);
		given(trade.getId()).willReturn(900L);
		given(tradeService.createTrade(auction, bidder, TradeType.BUY_NOW, 50_000)).willReturn(trade);

		BidResultResponse result = bidService.placeBid(AUCTION_ID, bidder.getId(), 70_000);

		then(pointService).should().lockForBid(AUCTION_ID, bidder.getId(), 50_000, prevBidder.getId(), 12_000);
		assertThat(auction.getStatus()).isEqualTo(AuctionStatus.BOUGHT_NOW);
		assertThat(result.amount()).isEqualTo(50_000);
		assertThat(result.boughtNow()).isTrue();
		assertThat(result.tradeId()).isEqualTo(900L);
		// 즉시구매로 밀려나도 OUTBID 알림, 금액은 실제 처리된 즉시구매가
		then(eventPublisher).should().publishEvent(new OutbidEvent(AUCTION_ID, "빈티지 카메라", prevBidder.getId(), 50_000));
	}

	@Test
	void 최소_입찰가보다_낮아도_즉시구매가_이상이면_즉시구매로_처리한다() {
		// 현재가 49,000 + 단위 1,000 = 최소 50,000 이지만 즉시구매가는 49,500
		Auction auction = auction(49_000, 1, prevBidder, 49_500L);
		givenAuctionAndBidder(auction, bidder);
		givenSaveAssignsId();
		given(tradeService.createTrade(any(), any(), any(), anyLong())).willReturn(mock(Trade.class));

		BidResultResponse result = bidService.placeBid(AUCTION_ID, bidder.getId(), 49_500);

		assertThat(result.boughtNow()).isTrue();
		assertThat(result.amount()).isEqualTo(49_500);
	}

	@Test
	void 최소_입찰가보다_낮으면_BID_TOO_LOW이고_포인트를_건드리지_않는다() {
		givenAuctionAndBidder(auction(12_000, 1, prevBidder, null), bidder);

		assertError(() -> bidService.placeBid(AUCTION_ID, bidder.getId(), 12_999), BidErrorCode.BID_TOO_LOW);
		then(pointService).shouldHaveNoInteractions();
		then(bidRepository).shouldHaveNoInteractions();
		then(eventPublisher).shouldHaveNoInteractions();
	}

	@Test
	void 마감_시각이_지났으면_AUCTION_CLOSED() {
		Auction auction = auction(10_000, 0, null, null);
		ReflectionTestUtils.setField(auction, "endAt", NOW);
		given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));

		assertError(() -> bidService.placeBid(AUCTION_ID, bidder.getId(), 10_000), BidErrorCode.AUCTION_CLOSED);
	}

	@Test
	void 판매자_본인은_입찰할_수_없다() {
		givenAuctionAndBidder(auction(10_000, 0, null, null), seller);

		assertError(() -> bidService.placeBid(AUCTION_ID, seller.getId(), 10_000), BidErrorCode.SELF_BID_NOT_ALLOWED);
	}

	@Test
	void 현재_최고_입찰자는_다시_입찰할_수_없다() {
		givenAuctionAndBidder(auction(12_000, 1, prevBidder, null), prevBidder);

		assertError(() -> bidService.placeBid(AUCTION_ID, prevBidder.getId(), 20_000),
			BidErrorCode.ALREADY_HIGHEST_BIDDER);
	}

	@Test
	void 배송지가_없으면_ADDRESS_REQUIRED() {
		User noAddress = user(4L, false);
		givenAuctionAndBidder(auction(10_000, 0, null, null), noAddress);

		assertError(() -> bidService.placeBid(AUCTION_ID, noAddress.getId(), 10_000), BidErrorCode.ADDRESS_REQUIRED);
	}

	@Test
	void 없는_경매면_AUCTION_NOT_FOUND() {
		given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.empty());

		assertError(() -> bidService.placeBid(AUCTION_ID, bidder.getId(), 10_000), AuctionErrorCode.AUCTION_NOT_FOUND);
	}

	// ---- 즉시구매 (R-44: 즉시구매가로 입찰한 것과 같다) ----

	@Test
	void 즉시구매는_즉시구매가로_입찰한_것과_같게_낙찰_거래_알림까지_처리한다() {
		Auction auction = auction(12_000, 1, prevBidder, 50_000L);
		givenAuctionAndBidder(auction, bidder);
		givenSaveAssignsId();
		Trade trade = mock(Trade.class);
		given(trade.getId()).willReturn(900L);
		given(tradeService.createTrade(auction, bidder, TradeType.BUY_NOW, 50_000)).willReturn(trade);

		BidResultResponse result = bidService.buyNow(AUCTION_ID, bidder.getId());

		then(pointService).should().lockForBid(AUCTION_ID, bidder.getId(), 50_000, prevBidder.getId(), 12_000);
		then(eventPublisher).should().publishEvent(new OutbidEvent(AUCTION_ID, "빈티지 카메라", prevBidder.getId(), 50_000));
		assertThat(auction.getStatus()).isEqualTo(AuctionStatus.BOUGHT_NOW);
		assertThat(result.amount()).isEqualTo(50_000);
		assertThat(result.boughtNow()).isTrue();
		assertThat(result.tradeId()).isEqualTo(900L);
	}

	@Test
	void 즉시구매가가_없는_경매는_BUY_NOW_NOT_AVAILABLE() {
		given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction(12_000, 1, prevBidder, null)));

		assertError(() -> bidService.buyNow(AUCTION_ID, bidder.getId()), BidErrorCode.BUY_NOW_NOT_AVAILABLE);
		then(pointService).shouldHaveNoInteractions();
	}

	@Test
	void 현재가가_이미_즉시구매가에_도달했으면_BUY_NOW_NOT_AVAILABLE() {
		given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction(50_000, 3, prevBidder, 50_000L)));

		assertError(() -> bidService.buyNow(AUCTION_ID, bidder.getId()), BidErrorCode.BUY_NOW_NOT_AVAILABLE);
	}

	@Test
	void 마감된_경매의_즉시구매는_입찰과_같이_AUCTION_CLOSED() {
		Auction auction = auction(12_000, 1, prevBidder, 50_000L);
		ReflectionTestUtils.setField(auction, "endAt", NOW);
		given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));

		assertError(() -> bidService.buyNow(AUCTION_ID, bidder.getId()), BidErrorCode.AUCTION_CLOSED);
	}

	@Test
	void 현재_최고_입찰자는_즉시구매도_할_수_없다() {
		// R-11: 즉시구매도 입찰로 본다
		givenAuctionAndBidder(auction(12_000, 1, prevBidder, 50_000L), prevBidder);

		assertError(() -> bidService.buyNow(AUCTION_ID, prevBidder.getId()), BidErrorCode.ALREADY_HIGHEST_BIDDER);
		then(pointService).shouldHaveNoInteractions();
	}

	// ---- 준비 ----

	private void givenAuctionAndBidder(Auction auction, User user) {
		given(auctionRepository.findById(AUCTION_ID)).willReturn(Optional.of(auction));
		given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
	}

	private void givenSaveAssignsId() {
		given(bidRepository.save(any(Bid.class))).willAnswer(invocation -> {
			Bid bid = invocation.getArgument(0);
			ReflectionTestUtils.setField(bid, "id", 500L);
			return bid;
		});
	}

	private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
		assertThatThrownBy(call)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(code);
	}

	// ---- 테스트용 엔티티 (아직 생성자·팩토리가 없어 리플렉션으로 만든다) ----

	/** ACTIVE, 시작가 10,000, 마감 END_AT, 연장(30분 전 감지, 60분 연장) 켜짐 */
	private Auction auction(long currentPrice, int bidCount, User highestBidder, Long buyNowPrice) {
		Auction auction = newEntity(Auction.class);
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);
		ReflectionTestUtils.setField(auction, "seller", seller);
		ReflectionTestUtils.setField(auction, "title", "빈티지 카메라");
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.ACTIVE);
		ReflectionTestUtils.setField(auction, "startPrice", 10_000L);
		ReflectionTestUtils.setField(auction, "currentPrice", currentPrice);
		ReflectionTestUtils.setField(auction, "bidCount", bidCount);
		ReflectionTestUtils.setField(auction, "highestBidder", highestBidder);
		ReflectionTestUtils.setField(auction, "buyNowPrice", buyNowPrice);
		ReflectionTestUtils.setField(auction, "endAt", END_AT);
		ReflectionTestUtils.setField(auction, "originalEndAt", END_AT);
		ReflectionTestUtils.setField(auction, "extendEnabled", true);
		ReflectionTestUtils.setField(auction, "extendTriggerMinutes", 30);
		ReflectionTestUtils.setField(auction, "extendMinutes", 60);
		return auction;
	}

	private static User user(Long id, boolean hasAddress) {
		User user = newEntity(User.class);
		ReflectionTestUtils.setField(user, "id", id);
		if (hasAddress) {
			ReflectionTestUtils.setField(user, "address", newEntity(Address.class));
		}
		return user;
	}

	private static <T> T newEntity(Class<T> type) {
		try {
			return BeanUtils.instantiateClass(type.getDeclaredConstructor());
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}
}
