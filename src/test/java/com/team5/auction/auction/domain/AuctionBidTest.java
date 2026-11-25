package com.team5.auction.auction.domain;

import static com.team5.auction.auction.domain.AuctionFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.team5.auction.user.domain.User;

/** Auction의 입찰 담당 메서드 (계약서 4장). 시간은 고정값을 넘긴다. */
class AuctionBidTest {

	@Nested
	class isOpenAt {

		@Test
		void 마감_1초_전이면_열려_있다() {
			assertThat(active().build().isOpenAt(END_AT.minusSeconds(1))).isTrue();
		}

		@Test
		void 마감_정각이면_닫혀_있다() {
			assertThat(active().build().isOpenAt(END_AT)).isFalse();
		}

		@Test
		void ACTIVE가_아니면_마감_전이어도_닫혀_있다() {
			Auction auction = active().status(AuctionStatus.BOUGHT_NOW).build();

			assertThat(auction.isOpenAt(END_AT.minusHours(1))).isFalse();
		}
	}

	@Nested
	class minimumBid {

		@Test
		void 입찰이_없으면_시작가() {
			assertThat(active().build().minimumBid(1_000)).isEqualTo(10_000);
		}

		@Test
		void 입찰이_있으면_현재가_더하기_입찰_단위() {
			Auction auction = active().currentPrice(15_000).bidCount(1).build();

			assertThat(auction.minimumBid(1_000)).isEqualTo(16_000);
		}
	}

	@Nested
	class canBuyNow {

		@Test
		void 즉시구매가가_없으면_불가() {
			assertThat(active().buyNowPrice(null).build().canBuyNow()).isFalse();
		}

		@Test
		void 현재가가_즉시구매가보다_낮으면_가능() {
			assertThat(active().buyNowPrice(50_000L).currentPrice(49_999).build().canBuyNow()).isTrue();
		}

		@Test
		void 현재가가_즉시구매가와_같으면_불가() {
			assertThat(active().buyNowPrice(50_000L).currentPrice(50_000).build().canBuyNow()).isFalse();
		}
	}

	@Nested
	class reachesBuyNow {

		@Test
		void 즉시구매가_이상이면_true() {
			Auction auction = active().buyNowPrice(50_000L).currentPrice(20_000).build();

			assertThat(auction.reachesBuyNow(50_000)).isTrue();
			assertThat(auction.reachesBuyNow(70_000)).isTrue();
		}

		@Test
		void 즉시구매가_미만이면_false() {
			assertThat(active().buyNowPrice(50_000L).build().reachesBuyNow(49_999)).isFalse();
		}

		@Test
		void 즉시구매가가_없으면_false() {
			assertThat(active().buyNowPrice(null).build().reachesBuyNow(1_000_000)).isFalse();
		}
	}

	@Nested
	class isSeller {

		@Test
		void 판매자_id면_true_아니면_false() {
			User seller = mock(User.class);
			given(seller.getId()).willReturn(1L);
			Auction auction = active().seller(seller).build();

			assertThat(auction.isSeller(1L)).isTrue();
			assertThat(auction.isSeller(2L)).isFalse();
			assertThat(auction.isSeller(null)).isFalse();
		}
	}

	@Nested
	class applyBid {

		private final User bidder = mock(User.class);

		@Test
		void 현재가_최고입찰자_입찰수를_갱신한다() {
			Auction auction = active().build();

			auction.applyBid(bidder, 12_000, END_AT.minusDays(1));

			assertThat(auction.getCurrentPrice()).isEqualTo(12_000);
			assertThat(auction.getHighestBidder()).isSameAs(bidder);
			assertThat(auction.getBidCount()).isEqualTo(1);
		}

		@Test
		void 감지_구간_밖이면_연장하지_않는다() {
			Auction auction = active().build();

			boolean extended = auction.applyBid(bidder, 12_000, END_AT.minusMinutes(31));

			assertThat(extended).isFalse();
			assertThat(auction.getEndAt()).isEqualTo(END_AT);
			assertThat(auction.getExtendCount()).isZero();
		}

		@Test
		void 감지_구간_시작_정각이면_연장한다() {
			Auction auction = active().build();
			var now = END_AT.minusMinutes(30);

			boolean extended = auction.applyBid(bidder, 12_000, now);

			assertThat(extended).isTrue();
			assertThat(auction.getEndAt()).isEqualTo(now.plusMinutes(60));
			assertThat(auction.getExtendCount()).isEqualTo(1);
		}

		@Test
		void 감지_구간_안이면_입찰_시각_더하기_60분으로_연장하고_원래_마감은_유지한다() {
			Auction auction = active().build();
			var now = END_AT.minusMinutes(5);

			auction.applyBid(bidder, 12_000, now);

			assertThat(auction.getEndAt()).isEqualTo(now.plusMinutes(60));
			assertThat(auction.getOriginalEndAt()).isEqualTo(END_AT);
		}

		@Test
		void 연장이_꺼져_있으면_연장하지_않는다() {
			Auction auction = active().extension(false, 30, 60).build();

			auction.applyBid(bidder, 12_000, END_AT.minusMinutes(5));

			assertThat(auction.getEndAt()).isEqualTo(END_AT);
			assertThat(auction.getExtendCount()).isZero();
		}

		@Test
		void 연장_시간이_감지_구간보다_짧아도_마감이_앞당겨지지_않는다() {
			Auction auction = active().extension(true, 30, 10).build();

			boolean extended = auction.applyBid(bidder, 12_000, END_AT.minusMinutes(25));

			assertThat(extended).isFalse();
			assertThat(auction.getEndAt()).isEqualTo(END_AT);
			assertThat(auction.getExtendCount()).isZero();
		}

		@Test
		void 연장은_여러_번_될_수_있다() {
			Auction auction = active().build();
			var first = END_AT.minusMinutes(5);
			var second = first.plusMinutes(50);

			auction.applyBid(bidder, 12_000, first);
			auction.applyBid(bidder, 13_000, second);

			assertThat(auction.getEndAt()).isEqualTo(second.plusMinutes(60));
			assertThat(auction.getExtendCount()).isEqualTo(2);
		}
	}

	@Nested
	class markBoughtNow {

		@Test
		void ACTIVE면_BOUGHT_NOW가_된다() {
			Auction auction = active().build();

			auction.markBoughtNow();

			assertThat(auction.getStatus()).isEqualTo(AuctionStatus.BOUGHT_NOW);
		}

		@Test
		void ACTIVE가_아니면_코드_버그로_보고_예외() {
			Auction auction = active().status(AuctionStatus.SOLD).build();

			assertThatThrownBy(auction::markBoughtNow).isInstanceOf(IllegalStateException.class);
		}
	}
}
