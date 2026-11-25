package com.team5.auction.auction.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionImage;
import com.team5.auction.auction.dto.AuctionCreateRequest;
import com.team5.auction.auction.exception.AuctionErrorCode;
import com.team5.auction.auction.repository.AuctionRepository;
import com.team5.auction.category.domain.Category;
import com.team5.auction.category.exception.CategoryErrorCode;
import com.team5.auction.category.repository.CategoryRepository;
import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.exception.ErrorCode;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.repository.UserRepository;

/** 경매 등록 서비스. 저장소는 Mockito로 대신하고, 시간은 Clock.fixed로 고정합니다. DB 없이 돕니다. */
@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 10, 0);
	private static final List<String> ONE_IMAGE = List.of("https://example.com/a.jpg");

	@Mock
	private AuctionRepository auctionRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private User seller;
	@Mock
	private Category category;

	private AuctionService auctionService;

	@BeforeEach
	void setUp() {
		Clock fixedClock = Clock.fixed(ZonedDateTime.of(NOW, ZONE).toInstant(), ZONE);
		auctionService = new AuctionService(fixedClock, policy(true, 30, 60),
			auctionRepository, userRepository, categoryRepository);
	}

	@Test
	void 경매를_등록하면_입력값과_현재시각으로_저장된다() {
		givenSellerAndCategoryExist();

		Auction saved = auctionService.create(1L, request(7, List.of("https://example.com/a.jpg")));

		assertThat(saved.getSeller()).isSameAs(seller);
		assertThat(saved.getCategory()).isSameAs(category);
		assertThat(saved.getTitle()).isEqualTo("맥북 프로");
		assertThat(saved.getStartPrice()).isEqualTo(10_000L);
		assertThat(saved.getCurrentPrice()).isEqualTo(10_000L);
		assertThat(saved.getStartAt()).isEqualTo(NOW);
		assertThat(saved.getEndAt()).isEqualTo(NOW.plusDays(7));
		then(auctionRepository).should().save(saved);
	}

	@Test
	void yml의_연장_설정값을_경매에_복사한다() {
		givenSellerAndCategoryExist();

		Auction saved = auctionService.create(1L, request(7, List.of("https://example.com/a.jpg")));

		assertThat(saved.isExtendEnabled()).isTrue();
		assertThat(saved.getExtendTriggerMinutes()).isEqualTo(30);
		assertThat(saved.getExtendMinutes()).isEqualTo(60);
	}

	@Test
	void 등록_후_설정값이_바뀌어도_이미_등록된_경매의_값은_그대로다() {
		givenSellerAndCategoryExist();
		Auction before = auctionService.create(1L, request(7, List.of("https://example.com/a.jpg")));

		AuctionService changedPolicyService = new AuctionService(Clock.system(ZONE), policy(false, 10, 5),
			auctionRepository, userRepository, categoryRepository);
		Auction after = changedPolicyService.create(1L, request(7, List.of("https://example.com/a.jpg")));

		assertThat(before.isExtendEnabled()).isTrue();
		assertThat(before.getExtendTriggerMinutes()).isEqualTo(30);
		assertThat(before.getExtendMinutes()).isEqualTo(60);
		assertThat(after.isExtendEnabled()).isFalse();
		assertThat(after.getExtendMinutes()).isEqualTo(5);
	}

	@Test
	void 이미지는_요청_순서대로_0부터_순번이_매겨진다() {
		givenSellerAndCategoryExist();

		Auction saved = auctionService.create(1L, request(7, List.of(
			"https://example.com/a.jpg",
			"https://example.com/b.jpg",
			"https://example.com/c.jpg")));

		assertThat(saved.getImages())
			.extracting(AuctionImage::getUrl, AuctionImage::getSortOrder)
			.containsExactly(
				tuple("https://example.com/a.jpg", 0),
				tuple("https://example.com/b.jpg", 1),
				tuple("https://example.com/c.jpg", 2));
	}

	@Test
	void 회원이_없으면_예외가_나고_저장하지_않는다() {
		given(userRepository.findById(1L)).willReturn(Optional.empty());

		assertErrorCode(() -> auctionService.create(1L, request(7, List.of("https://example.com/a.jpg"))),
			UserErrorCode.USER_NOT_FOUND);
		then(auctionRepository).should(never()).save(any());
	}

	@Test
	void 카테고리가_없으면_예외가_나고_저장하지_않는다() {
		given(userRepository.findById(1L)).willReturn(Optional.of(seller));
		given(categoryRepository.findById(10L)).willReturn(Optional.empty());

		assertErrorCode(() -> auctionService.create(1L, request(7, List.of("https://example.com/a.jpg"))),
			CategoryErrorCode.CATEGORY_NOT_FOUND);
		then(auctionRepository).should(never()).save(any());
	}

	// ---- 등록 검증 (R-02, R-03, R-08, R-09) ----

	@ParameterizedTest
	@ValueSource(ints = {0, 15})
	void 기간이_1일_미만이거나_14일을_넘으면_AUCTION_PERIOD_INVALID(int durationDays) {
		assertErrorCode(() -> auctionService.create(1L, request(durationDays, ONE_IMAGE)),
			AuctionErrorCode.AUCTION_PERIOD_INVALID);
		then(userRepository).shouldHaveNoInteractions();
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 14})
	void 기간이_1일이나_14일이면_등록된다(int durationDays) {
		givenSellerAndCategoryExist();

		Auction saved = auctionService.create(1L, request(durationDays, ONE_IMAGE));

		assertThat(saved.getEndAt()).isEqualTo(NOW.plusDays(durationDays));
	}

	@ParameterizedTest
	@ValueSource(longs = {900L, 1_050L})
	void 시작가가_1000원_미만이거나_100원_단위가_아니면_PRICE_UNIT_INVALID(long startPrice) {
		assertErrorCode(() -> auctionService.create(1L, priceRequest(startPrice, null)),
			AuctionErrorCode.PRICE_UNIT_INVALID);
	}

	@ParameterizedTest
	@ValueSource(longs = {0L, 900L, 20_050L})
	void 즉시구매가가_1000원_미만이거나_100원_단위가_아니면_PRICE_UNIT_INVALID(long buyNowPrice) {
		assertErrorCode(() -> auctionService.create(1L, priceRequest(10_000L, buyNowPrice)),
			AuctionErrorCode.PRICE_UNIT_INVALID);
	}

	@ParameterizedTest
	@ValueSource(longs = {5_000L, 10_000L})
	void 즉시구매가가_시작가보다_작거나_같으면_BUY_NOW_PRICE_INVALID(long buyNowPrice) {
		assertErrorCode(() -> auctionService.create(1L, priceRequest(10_000L, buyNowPrice)),
			AuctionErrorCode.BUY_NOW_PRICE_INVALID);
	}

	@Test
	void 즉시구매가가_없으면_가격_비교없이_등록된다() {
		givenSellerAndCategoryExist();

		Auction saved = auctionService.create(1L, priceRequest(1_000L, null));

		assertThat(saved.getStartPrice()).isEqualTo(1_000L);
		assertThat(saved.getBuyNowPrice()).isNull();
	}

	@Test
	void 이미지가_없으면_IMAGE_COUNT_INVALID() {
		assertErrorCode(() -> auctionService.create(1L, request(7, null)),
			AuctionErrorCode.IMAGE_COUNT_INVALID);
		assertErrorCode(() -> auctionService.create(1L, request(7, List.of())),
			AuctionErrorCode.IMAGE_COUNT_INVALID);
	}

	@Test
	void 이미지가_10장을_넘으면_IMAGE_COUNT_INVALID() {
		List<String> elevenImages = Collections.nCopies(11, "https://example.com/a.jpg");

		assertErrorCode(() -> auctionService.create(1L, request(7, elevenImages)),
			AuctionErrorCode.IMAGE_COUNT_INVALID);
	}

	@Test
	void 이미지가_10장이면_등록된다() {
		givenSellerAndCategoryExist();

		Auction saved = auctionService.create(1L, request(7, Collections.nCopies(10, "https://example.com/a.jpg")));

		assertThat(saved.getImages()).hasSize(10);
	}

	private void assertErrorCode(ThrowingCallable call, ErrorCode expected) {
		assertThatThrownBy(call)
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).getErrorCode())
			.isEqualTo(expected);
	}

	private void givenSellerAndCategoryExist() {
		given(userRepository.findById(1L)).willReturn(Optional.of(seller));
		given(categoryRepository.findById(10L)).willReturn(Optional.of(category));
		given(auctionRepository.save(any(Auction.class))).willAnswer(invocation -> invocation.getArgument(0));
	}

	private AuctionCreateRequest request(int durationDays, List<String> imageUrls) {
		return new AuctionCreateRequest(10L, "맥북 프로", "거의 새것", 10_000L, 50_000L, durationDays, imageUrls);
	}

	private AuctionCreateRequest priceRequest(long startPrice, Long buyNowPrice) {
		return new AuctionCreateRequest(10L, "맥북 프로", "거의 새것", startPrice, buyNowPrice, 7, ONE_IMAGE);
	}

	private AuctionPolicyProperties policy(boolean enabled, int triggerMinutes, int extendMinutes) {
		return new AuctionPolicyProperties(
			new AuctionPolicyProperties.Fee(List.of(new AuctionPolicyProperties.FeeTier(null, new BigDecimal("0.05")))),
			new AuctionPolicyProperties.BidUnit(List.of(new AuctionPolicyProperties.BidUnitTier(null, 1000))),
			new AuctionPolicyProperties.Extension(enabled, triggerMinutes, extendMinutes),
			new AuctionPolicyProperties.TradePolicy(3, 7),
			new AuctionPolicyProperties.PointPolicy(1_000_000L));
	}
}
