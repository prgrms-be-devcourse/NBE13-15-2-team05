package com.team5.auction.user.domain;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.point.exception.PointErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserBalanceTest {

    @Nested
    class charge {

        @Test
        void 충전하면_available이_늘어난다() {
            User user = user();

            user.charge(10_000);

            assertBalance(user, 10_000, 0);
        }

        @Test
        void 금액이_0_이하면_IllegalArgumentException() {
            User user = user();

            assertThatThrownBy(() -> user.charge(0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertBalance(user, 0, 0);
        }
    }

    @Nested
    class lock {

        @Test
        void available에서_locked로_옮긴다() {
            User user = userWith(10_000);

            user.lock(4_000);

            assertBalance(user, 6_000, 4_000);
        }

        @Test
        void 가진_포인트를_전부_묶을_수_있다() {
            User user = userWith(10_000);

            user.lock(10_000);

            assertBalance(user, 0, 10_000);
        }

        @Test
        void available이_모자라면_BALANCE_INSUFFICIENT이고_잔액은_그대로다() {
            User user = userWith(10_000);

            assertThatThrownBy(() -> user.lock(10_001))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(PointErrorCode.BALANCE_INSUFFICIENT);
            assertBalance(user, 10_000, 0);
        }
    }

    @Nested
    class unlock {

        @Test
        void locked에서_available로_돌려준다() {
            User user = userWith(10_000);
            user.lock(4_000);

            user.unlock(4_000);

            assertBalance(user, 10_000, 0);
        }

        @Test
        void locked가_모자라면_IllegalStateException이고_잔액은_그대로다() {
            User user = userWith(10_000);
            user.lock(4_000);

            assertThatThrownBy(() -> user.unlock(4_001))
                    .isInstanceOf(IllegalStateException.class);
            assertBalance(user, 6_000, 4_000);
        }
    }

    @Nested
    class deductLocked {

        @Test
        void locked에서_뺀다() {
            User user = userWith(10_000);
            user.lock(4_000);

            user.deductLocked(4_000);

            assertBalance(user, 6_000, 0);
        }

        @Test
        void locked가_모자라면_IllegalStateException이고_잔액은_그대로다() {
            User user = userWith(10_000);
            user.lock(4_000);

            assertThatThrownBy(() -> user.deductLocked(4_001))
                    .isInstanceOf(IllegalStateException.class);
            assertBalance(user, 6_000, 4_000);
        }
    }

    @Nested
    class addAvailable {

        @Test
        void 정산금만큼_available이_늘어난다() {
            User user = user();

            user.addAvailable(14_250);

            assertBalance(user, 14_250, 0);
        }
    }

    /** 테스트용 회원. 잔액 0에서 시작합니다 */
    private static User user() {
        return User.createUser("test@test.com", "password", "tester", null);
    }
    /** available을 채워 둔 회원 */
    private static User userWith(long available) {
        User user = user();
        user.charge(available);
        return user;
    }

    private static void assertBalance(User user, long available, long locked) {
        assertThat(user.getAvailableBalance()).isEqualTo(available);
        assertThat(user.getLockedBalance()).isEqualTo(locked);
    }
}
