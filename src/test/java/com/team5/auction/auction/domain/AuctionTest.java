package com.team5.auction.auction.domain;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.team5.auction.category.domain.Category;
import com.team5.auction.user.domain.User;

/** Auction 생성(builder)과 이미지 추가를 확인합니다. DB 없이 돕니다. */
class AuctionTest {

	private static final LocalDateTime START_AT = LocalDateTime.of(2026, 11, 25, 10, 0);

	@Test
	void 생성하면_진행중_상태이고_현재가는_시작가다() {
		Auction auction = createAuction(7);

		assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
		assertThat(auction.getCurrentPrice()).isEqualTo(10_000L);
		assertThat(auction.getStartPrice()).isEqualTo(10_000L);
		assertThat(auction.getBuyNowPrice()).isEqualTo(50_000L);
		assertThat(auction.getBidCount()).isZero();
		assertThat(auction.getHighestBidder()).isNull();
	}

	@Test
	void 마감시각은_시작시각에_기간을_더한_값이고_원래_마감시각과_같다() {
		Auction auction = createAuction(7);

		assertThat(auction.getStartAt()).isEqualTo(START_AT);
		assertThat(auction.getEndAt()).isEqualTo(START_AT.plusDays(7));
		assertThat(auction.getOriginalEndAt()).isEqualTo(auction.getEndAt());
	}

	@Test
	void 연장_설정값을_그대로_복사하고_연장횟수는_0이다() {
		Auction auction = createAuction(7);

		assertThat(auction.isExtendEnabled()).isTrue();
		assertThat(auction.getExtendTriggerMinutes()).isEqualTo(30);
		assertThat(auction.getExtendMinutes()).isEqualTo(60);
		assertThat(auction.getExtendCount()).isZero();
	}

	@Test
	void 이미지를_추가하면_경매와_양방향으로_연결되고_순서가_저장된다() {
		Auction auction = createAuction(7);

		auction.addImage("https://example.com/a.jpg", 0);
		auction.addImage("https://example.com/b.jpg", 1);

		assertThat(auction.getImages()).hasSize(2);
		assertThat(auction.getImages())
			.extracting(AuctionImage::getUrl, AuctionImage::getSortOrder)
			.containsExactly(
				tuple("https://example.com/a.jpg", 0),
				tuple("https://example.com/b.jpg", 1));
		assertThat(auction.getImages())
			.allSatisfy(image -> assertThat(image.getAuction()).isSameAs(auction));
	}

	private Auction createAuction(int durationDays) {
		return Auction.builder()
			.seller(mock(User.class))
			.category(mock(Category.class))
			.title("맥북 프로")
			.description("거의 새것")
			.startPrice(10_000L)
			.buyNowPrice(50_000L)
			.startAt(START_AT)
			.durationDays(durationDays)
			.extendEnabled(true)
			.extendTriggerMinutes(30)
			.extendMinutes(60)
			.build();
	}
}
