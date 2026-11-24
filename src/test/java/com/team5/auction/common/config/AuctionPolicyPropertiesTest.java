package com.team5.auction.common.config;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/** application.yml의 정책 설정값이 의도대로 바인딩되는지 확인합니다. DB 없이 돕니다. */
class AuctionPolicyPropertiesTest {

	@Test
	void application_yml_정책값이_바인딩된다() throws Exception {
		AuctionPolicyProperties p = bind();

		assertThat(p.fee().tiers()).hasSize(2);
		assertThat(p.fee().tiers().get(0).rate()).isEqualByComparingTo(new BigDecimal("0.05"));
		assertThat(p.fee().tiers().get(1).maxPrice()).isNull();
		assertThat(p.bidUnit().tiers()).hasSize(4);
		assertThat(p.bidUnit().tiers().get(3).maxPrice()).isNull();
		assertThat(p.extension().triggerMinutes()).isEqualTo(30);
		assertThat(p.trade().shipDeadlineDays()).isEqualTo(3);
		assertThat(p.point().maxChargeAmount()).isEqualTo(1_000_000L);
	}

	@Test
	void 마지막_구간에_상한이_있으면_실패한다() {
		assertThatThrownBy(() -> new AuctionPolicyProperties.BidUnit(List.of(
			new AuctionPolicyProperties.BidUnitTier(9999L, 500))))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private AuctionPolicyProperties bind() throws Exception {
		StandardEnvironment env = new StandardEnvironment();
		for (PropertySource<?> source : new YamlPropertySourceLoader()
			.load("application", new ClassPathResource("application.yml"))) {
			env.getPropertySources().addLast(source);
		}
		return new Binder(ConfigurationPropertySources.get(env))
			.bind("auction", AuctionPolicyProperties.class)
			.get();
	}
}
