package com.team5.auction.auction.domain;

import java.time.LocalDateTime;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * 테스트 전용 Auction 생성기.
 * Auction에 아직 생성자·팩토리가 없어(경매 담당 작업) 리플렉션으로 필드를 채운다. 팩토리가 생기면 교체.
 */
public class AuctionFixture {

	public static final LocalDateTime END_AT = LocalDateTime.of(2026, 11, 30, 20, 0);

	private final Auction auction = new Auction();

	private AuctionFixture() {
		set("status", AuctionStatus.ACTIVE);
		set("startPrice", 10_000L);
		set("currentPrice", 10_000L);
		set("bidCount", 0);
		set("startAt", END_AT.minusDays(3));
		set("endAt", END_AT);
		set("originalEndAt", END_AT);
		set("extendEnabled", true);
		set("extendTriggerMinutes", 30);
		set("extendMinutes", 60);
		set("extendCount", 0);
	}

	/** ACTIVE, 시작가 10,000, 입찰 0건, 마감 END_AT, 연장(30분 전 감지, 60분 연장) 켜짐 */
	public static AuctionFixture active() {
		return new AuctionFixture();
	}

	public AuctionFixture status(AuctionStatus status) {
		return set("status", status);
	}

	public AuctionFixture currentPrice(long currentPrice) {
		return set("currentPrice", currentPrice);
	}

	public AuctionFixture bidCount(int bidCount) {
		return set("bidCount", bidCount);
	}

	public AuctionFixture buyNowPrice(Long buyNowPrice) {
		return set("buyNowPrice", buyNowPrice);
	}

	public AuctionFixture extension(boolean enabled, int triggerMinutes, int extendMinutes) {
		set("extendEnabled", enabled);
		set("extendTriggerMinutes", triggerMinutes);
		return set("extendMinutes", extendMinutes);
	}

	public Auction build() {
		return auction;
	}

	private AuctionFixture set(String field, Object value) {
		ReflectionTestUtils.setField(auction, field, value);
		return this;
	}
}
