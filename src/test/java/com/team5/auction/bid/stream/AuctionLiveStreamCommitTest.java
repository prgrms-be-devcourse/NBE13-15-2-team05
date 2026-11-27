package com.team5.auction.bid.stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.team5.auction.TestcontainersConfiguration;
import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.bid.dto.AuctionLiveResponse;
import com.team5.auction.bid.event.BidPlacedEvent;

/** 실시간 현재가는 입찰이 커밋된 뒤에만 나가고, 롤백되면 나가지 않는다 (실제 트랜잭션으로 확인) */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuctionLiveStreamCommitTest {

	private static final BidPlacedEvent EVENT =
		new BidPlacedEvent(10L, 15_000, 16_000, 2, LocalDateTime.of(2026, 11, 27, 12, 0), false, AuctionStatus.ACTIVE);

	@MockitoSpyBean
	private AuctionLiveStream stream;
	@Autowired
	private ApplicationEventPublisher eventPublisher;
	@Autowired
	private TransactionTemplate transactionTemplate;

	@Test
	void 커밋되면_구독자에게_보낸다() {
		transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(EVENT));

		then(stream).should().broadcast(eq(10L), eq(AuctionLiveResponse.from(EVENT)));
	}

	@Test
	void 롤백되면_보내지_않는다() {
		transactionTemplate.executeWithoutResult(status -> {
			eventPublisher.publishEvent(EVENT);
			status.setRollbackOnly();
		});

		then(stream).should(never()).broadcast(any(), any());
	}
}
