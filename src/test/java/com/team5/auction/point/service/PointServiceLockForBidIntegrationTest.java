package com.team5.auction.point.service;

import com.team5.auction.TestcontainersConfiguration;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.point.exception.PointErrorCode;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * lockForBid를 실제 MySQL로 확인한다 (PR #24 리뷰: 한 회원이 서로 다른 경매에 동시에 입찰).
 * 목으로는 JPA 1차 캐시와 DB 락이 맞물리는 동작을 볼 수 없다.
 * 테스트에 @Transactional을 붙이지 않는다. 두 트랜잭션이 각자 커밋해야 문제가 생기는 순서를 재현할 수 있다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PointServiceLockForBidIntegrationTest {

    @Autowired
    private PointService pointService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private EntityManager entityManager;

    @Test
    void 먼저_읽어_둔_회원이라도_락을_건_뒤의_최신_잔액으로_묶는다() {
        Long userId = saveUser(100_000, 0);
        TransactionTemplate bid = new TransactionTemplate(transactionManager);
        TransactionTemplate otherBid = new TransactionTemplate(transactionManager);
        otherBid.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> bid.executeWithoutResult(status -> {
            // 1. BidService처럼 락 없이 회원을 먼저 읽는다. 이때 사용 가능 포인트는 100,000
            userRepository.findById(userId).orElseThrow();
            // 2. 그사이 다른 입찰이 별도 트랜잭션에서 경매 10에 80,000을 묶고 커밋한다
            otherBid.executeWithoutResult(s -> pointService.lockForBid(10L, userId, 80_000, null, 0));
            // 3. 이 입찰이 경매 11에 80,000을 묶으려 한다. 남은 20,000으로는 모자라야 한다
            pointService.lockForBid(11L, userId, 80_000, null, 0);
        }))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PointErrorCode.BALANCE_INSUFFICIENT);

        // 실패한 입찰은 롤백되고, 먼저 커밋한 입찰만 남는다
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getAvailableBalance()).isEqualTo(20_000);
        assertThat(user.getLockedBalance()).isEqualTo(80_000);
    }

    @Test
    void 이전_입찰자를_프록시로_들고_있어도_최신_잔액으로_풀어_준다() {
        Long prevId = saveUser(100_000, 12_000);   // 경매 10에 12,000으로 입찰해 둔 이전 최고 입찰자 (88,000 / 12,000)
        Long bidderId = saveUser(100_000, 0);
        TransactionTemplate bid = new TransactionTemplate(transactionManager);
        TransactionTemplate otherBid = new TransactionTemplate(transactionManager);
        otherBid.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        bid.executeWithoutResult(status -> {
            // 1. BidService처럼 입찰자는 먼저 읽고, 이전 입찰자는 경매의 highestBidder처럼 id만 든 프록시로 들고 있다
            userRepository.findById(bidderId).orElseThrow();
            entityManager.getReference(User.class, prevId);
            // 2. 그사이 이전 입찰자가 다른 경매 20에 30,000을 묶고 커밋한다 (58,000 / 42,000)
            otherBid.executeWithoutResult(s -> pointService.lockForBid(20L, prevId, 30_000, null, 0));
            // 3. 경매 10에서 새 입찰자가 15,000으로 이기면서 이전 입찰자의 12,000을 푼다
            pointService.lockForBid(10L, bidderId, 15_000, prevId, 12_000);
        });

        // 경매 20에 묶은 30,000은 그대로 남아 있어야 한다
        User prev = userRepository.findById(prevId).orElseThrow();
        assertThat(prev.getAvailableBalance()).isEqualTo(70_000);
        assertThat(prev.getLockedBalance()).isEqualTo(30_000);
    }

    /** 포인트를 충전하고 locked만큼 묶어 둔 회원을 저장한다. 테스트가 실제로 커밋하므로 이메일과 닉네임이 겹치지 않게 만든다 */
    private Long saveUser(long charged, long locked) {
        String key = String.valueOf(System.nanoTime());
        User user = User.createUser("lock" + key + "@test.com", "password", "lock" + key, null);
        user.charge(charged);
        if (locked > 0) {
            user.lock(locked);
        }
        return userRepository.save(user).getId();
    }
}
