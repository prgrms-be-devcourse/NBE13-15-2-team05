package com.team5.auction.common.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

/**
 * 현재 시각은 항상 Clock 빈으로 얻습니다: LocalDateTime.now(clock). (계약서 1.5)
 * 테스트에서는 Clock.fixed(...)로 바꿔 시간을 고정합니다.
 */
@Configuration
public class TimeConfig {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	@Bean
	public Clock clock() {
		return Clock.system(ZONE);
	}

	/** createdAt, updatedAt도 같은 Clock을 씁니다. */
	@Bean
	public DateTimeProvider auditingDateTimeProvider(Clock clock) {
		return () -> Optional.of(LocalDateTime.now(clock));
	}
}
