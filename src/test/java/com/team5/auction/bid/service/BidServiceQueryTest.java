package com.team5.auction.bid.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.auction.exception.AuctionErrorCode;
import com.team5.auction.auction.repository.AuctionRepository;
import com.team5.auction.bid.domain.Bid;
import com.team5.auction.bid.dto.AdminBidResponse;
import com.team5.auction.bid.dto.BidHistoryResponse;
import com.team5.auction.bid.dto.MyBidResponse;
import com.team5.auction.bid.repository.BidRepository;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.user.domain.User;

/** 입찰 조회 3종. 저장소는 목으로 두고 응답 변환과 공개 범위(R-18)를 확인한다. */
@ExtendWith(MockitoExtension.class)
class BidServiceQueryTest {

	private static final Long AUCTION_ID = 10L;
	private static final LocalDateTime BID_AT = LocalDateTime.of(2026, 11, 25, 12, 0);
	private final Pageable pageable = PageRequest.of(0, 20);

	@Mock
	private BidRepository bidRepository;

	@Mock
	private AuctionRepository auctionRepository;

	@InjectMocks
	private BidService bidService;

	private final User me = user(1L, "나", "me@test.com");
	private final User other = user(2L, "다른사람", "other@test.com");
	private final Auction auction = auction(AUCTION_ID, "빈티지 카메라", 15_000, other);

	@Test
	void 입찰_내역은_본인_입찰만_mine으로_표시한다() {
		given(auctionRepository.existsById(AUCTION_ID)).willReturn(true);
		given(bidRepository.findByAuctionId(AUCTION_ID, pageable))
			.willReturn(page(bid(101L, other, 15_000), bid(100L, me, 12_000)));

		Page<BidHistoryResponse> result = bidService.getBids(AUCTION_ID, me.getId(), pageable);

		assertThat(result.getContent()).containsExactly(
			new BidHistoryResponse(15_000, BID_AT, false),
			new BidHistoryResponse(12_000, BID_AT, true));
	}

	@Test
	void 비로그인이면_mine은_모두_false() {
		given(auctionRepository.existsById(AUCTION_ID)).willReturn(true);
		given(bidRepository.findByAuctionId(AUCTION_ID, pageable)).willReturn(page(bid(100L, me, 12_000)));

		Page<BidHistoryResponse> result = bidService.getBids(AUCTION_ID, null, pageable);

		assertThat(result.getContent()).extracting(BidHistoryResponse::mine).containsOnly(false);
	}

	@Test
	void 없는_경매의_입찰_내역은_AUCTION_NOT_FOUND() {
		given(auctionRepository.existsById(AUCTION_ID)).willReturn(false);

		assertThatThrownBy(() -> bidService.getBids(AUCTION_ID, null, pageable))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
	}

	@Test
	void 내_입찰_목록은_경매_정보와_최고입찰자_여부를_담는다() {
		Auction leading = auction(11L, "노트북", 30_000, me);
		given(bidRepository.findWithAuctionByBidderId(me.getId(), pageable))
			.willReturn(page(bidOn(201L, leading, me, 30_000), bidOn(200L, auction, me, 12_000)));

		Page<MyBidResponse> result = bidService.getMyBids(me.getId(), pageable);

		assertThat(result.getContent()).containsExactly(
			new MyBidResponse(201L, 11L, "노트북", 30_000, BID_AT, AuctionStatus.ACTIVE, 30_000, true),
			new MyBidResponse(200L, AUCTION_ID, "빈티지 카메라", 12_000, BID_AT, AuctionStatus.ACTIVE, 15_000, false));
	}

	@Test
	void 운영자_입찰_내역은_실제_입찰자를_포함한다() {
		given(auctionRepository.existsById(AUCTION_ID)).willReturn(true);
		given(bidRepository.findWithBidderByAuctionId(AUCTION_ID, pageable)).willReturn(page(bid(100L, me, 12_000)));

		Page<AdminBidResponse> result = bidService.getBidsForAdmin(AUCTION_ID, pageable);

		assertThat(result.getContent()).containsExactly(
			new AdminBidResponse(100L, 12_000, BID_AT, new AdminBidResponse.Bidder(1L, "나", "me@test.com")));
	}

	@Test
	void 없는_경매의_운영자_입찰_내역은_AUCTION_NOT_FOUND() {
		given(auctionRepository.existsById(AUCTION_ID)).willReturn(false);

		assertThatThrownBy(() -> bidService.getBidsForAdmin(AUCTION_ID, pageable))
			.isInstanceOf(BusinessException.class);
	}

	// ---- 테스트용 엔티티 (아직 생성자·팩토리가 없어 리플렉션으로 만든다) ----

	private Page<Bid> page(Bid... bids) {
		return new PageImpl<>(List.of(bids), pageable, bids.length);
	}

	private Bid bid(Long id, User bidder, long amount) {
		return bidOn(id, auction, bidder, amount);
	}

	private static Bid bidOn(Long id, Auction auction, User bidder, long amount) {
		Bid bid = newEntity(Bid.class);
		ReflectionTestUtils.setField(bid, "id", id);
		ReflectionTestUtils.setField(bid, "auction", auction);
		ReflectionTestUtils.setField(bid, "bidder", bidder);
		ReflectionTestUtils.setField(bid, "amount", amount);
		ReflectionTestUtils.setField(bid, "bidAt", BID_AT);
		return bid;
	}

	private static Auction auction(Long id, String title, long currentPrice, User highestBidder) {
		Auction auction = newEntity(Auction.class);
		ReflectionTestUtils.setField(auction, "id", id);
		ReflectionTestUtils.setField(auction, "title", title);
		ReflectionTestUtils.setField(auction, "status", AuctionStatus.ACTIVE);
		ReflectionTestUtils.setField(auction, "currentPrice", currentPrice);
		ReflectionTestUtils.setField(auction, "highestBidder", highestBidder);
		return auction;
	}

	private static User user(Long id, String nickname, String email) {
		User user = newEntity(User.class);
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "nickname", nickname);
		ReflectionTestUtils.setField(user, "email", email);
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
