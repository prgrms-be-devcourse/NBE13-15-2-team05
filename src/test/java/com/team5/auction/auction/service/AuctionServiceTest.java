package com.team5.auction.auction.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionImage;
import com.team5.auction.auction.dto.AuctionCreateRequest;
import com.team5.auction.auction.repository.AuctionRepository;
import com.team5.auction.category.domain.Category;
import com.team5.auction.category.repository.CategoryRepository;
import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.repository.UserRepository;

/** 경매 등록 서비스. 저장소는 Mockito로 대신하고, 시간은 Clock.fixed로 고정합니다. DB 없이 돕니다. */
@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 10, 0);

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

		assertThatThrownBy(() -> auctionService.create(1L, request(7, List.of("https://example.com/a.jpg"))))
			.hasMessageContaining("USER_NOT_FOUND");
		then(auctionRepository).should(never()).save(any());
	}

	@Test
	void 카테고리가_없으면_예외가_나고_저장하지_않는다() {
		given(userRepository.findById(1L)).willReturn(Optional.of(seller));
		given(categoryRepository.findById(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> auctionService.create(1L, request(7, List.of("https://example.com/a.jpg"))))
			.hasMessageContaining("CATEGORY_NOT_FOUND");
		then(auctionRepository).should(never()).save(any());
	}

	private void givenSellerAndCategoryExist() {
		given(userRepository.findById(1L)).willReturn(Optional.of(seller));
		given(categoryRepository.findById(10L)).willReturn(Optional.of(category));
		given(auctionRepository.save(any(Auction.class))).willAnswer(invocation -> invocation.getArgument(0));
	}

	private AuctionCreateRequest request(int durationDays, List<String> imageUrls) {
		return new AuctionCreateRequest(10L, "맥북 프로", "거의 새것", 10_000L, 50_000L, durationDays, imageUrls);
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
