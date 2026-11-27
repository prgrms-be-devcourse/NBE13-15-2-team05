package com.team5.auction.bid.stream;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.team5.auction.bid.dto.AuctionLiveResponse;
import com.team5.auction.bid.event.BidPlacedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 경매별 실시간 현재가 구독(SSE) 관리.
 *
 * <p>입찰이 커밋되면 같은 경매를 보고 있는 모든 연결에 현재가를 보낸다.
 * 연결은 서버 메모리에 들고 있으므로 서버가 한 대라는 전제다. 여러 대가 되면 Redis pub/sub 등으로 바꿔야 한다.
 * 연결이 끝나거나(완료·시간 초과·오류) 전송에 실패하면 목록에서 뺀다.
 */
@Slf4j
@Component
public class AuctionLiveStream {

	static final String EVENT_NAME = "price";

	/** 시간이 지나면 끊고, 브라우저 EventSource가 알아서 다시 연결한다 */
	private static final long TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();

	private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

	/** 구독을 시작하고 현재 상태를 바로 한 번 보낸다 */
	public SseEmitter subscribe(Long auctionId, AuctionLiveResponse current) {
		SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
		emitters.computeIfAbsent(auctionId, id -> ConcurrentHashMap.newKeySet()).add(emitter);
		emitter.onCompletion(() -> remove(auctionId, emitter));
		emitter.onTimeout(() -> remove(auctionId, emitter));
		emitter.onError(e -> remove(auctionId, emitter));

		send(auctionId, emitter, SseEmitter.event().name(EVENT_NAME).data(current));
		return emitter;
	}

	/**
	 * 입찰 커밋 후 전송. 커밋 후 리스너에서 예외가 나면 이미 성공한 입찰 요청이 실패로 보이므로,
	 * 전송 실패는 여기서 모두 삼키고 해당 연결만 정리한다.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onBidPlaced(BidPlacedEvent event) {
		broadcast(event.auctionId(), AuctionLiveResponse.from(event));
	}

	public void broadcast(Long auctionId, AuctionLiveResponse live) {
		Set<SseEmitter> targets = emitters.get(auctionId);
		if (targets == null) {
			return;
		}
		for (SseEmitter emitter : targets) {
			send(auctionId, emitter, SseEmitter.event().name(EVENT_NAME).data(live));
		}
	}

	/** 조용한 연결을 프록시·브라우저가 끊지 않도록 주기적으로 빈 신호(주석)를 보낸다 */
	@Scheduled(fixedRate = 30_000)
	public void heartbeat() {
		emitters.forEach((auctionId, targets) ->
			targets.forEach(emitter -> send(auctionId, emitter, SseEmitter.event().comment("ping"))));
	}

	int subscriberCount(Long auctionId) {
		Set<SseEmitter> targets = emitters.get(auctionId);
		return targets == null ? 0 : targets.size();
	}

	private void send(Long auctionId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			emitter.send(event);
		} catch (IOException | IllegalStateException e) {
			// 클라이언트가 이미 떠났거나 연결이 끝난 경우. 정리만 하고 넘어간다
			log.debug("SSE send failed. auctionId={}", auctionId, e);
			remove(auctionId, emitter);
		}
	}

	private void remove(Long auctionId, SseEmitter emitter) {
		emitters.computeIfPresent(auctionId, (id, targets) -> {
			targets.remove(emitter);
			return targets.isEmpty() ? null : targets;
		});
	}
}
