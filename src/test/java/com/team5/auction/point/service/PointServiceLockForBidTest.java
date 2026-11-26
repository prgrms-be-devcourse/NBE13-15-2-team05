package com.team5.auction.point.service;

import com.team5.auction.common.entity.RefType;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.point.domain.BalanceHistory;
import com.team5.auction.point.domain.BalanceHistoryType;
import com.team5.auction.point.exception.PointErrorCode;
import com.team5.auction.point.repository.BalanceHistoryRepository;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/** lockForBid (계약서 2.1). 유저 조회와 내역 저장은 목으로 두고, 잔액과 저장된 내역을 확인한다 */
@ExtendWith(MockitoExtension.class)
class PointServiceLockForBidTest {

    private static final Long AUCTION_ID = 10L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private BalanceHistoryRepository balanceHistoryRepository;

    @InjectMocks
    private PointServiceImpl pointService;

    @Test
    void 첫_입찰이면_입찰자만_묶고_BID_LOCK_내역을_남긴다() {
        User bidder = user(3L, 100_000);
        given(userRepository.findAllByIdInForUpdate(List.of(3L))).willReturn(List.of(bidder));

        pointService.lockForBid(AUCTION_ID, 3L, 12_000, null, 0);

        assertBalance(bidder, 88_000, 12_000);
        ArgumentCaptor<BalanceHistory> captor = ArgumentCaptor.forClass(BalanceHistory.class);
        then(balanceHistoryRepository).should().save(captor.capture());
        assertHistory(captor.getValue(), bidder, BalanceHistoryType.BID_LOCK, 12_000, 88_000, 12_000);
    }

    @Test
    void 이전_입찰자가_있으면_새_입찰자는_묶고_이전_입찰자는_풀어_준다() {
        User prevBidder = user(2L, 100_000);
        prevBidder.lock(12_000); // 이전 입찰자가 12,000원에 입찰해 둔 상태
        User bidder = user(3L, 100_000);
        given(userRepository.findAllByIdInForUpdate(List.of(3L, 2L))).willReturn(List.of(prevBidder, bidder));

        pointService.lockForBid(AUCTION_ID, 3L, 15_000, 2L, 12_000);

        assertBalance(bidder, 85_000, 15_000);
        assertBalance(prevBidder, 100_000, 0);
        ArgumentCaptor<BalanceHistory> captor = ArgumentCaptor.forClass(BalanceHistory.class);
        then(balanceHistoryRepository).should(times(2)).save(captor.capture());
        List<BalanceHistory> histories = captor.getAllValues();
        assertHistory(histories.get(0), bidder, BalanceHistoryType.BID_LOCK, 15_000, 85_000, 15_000);
        assertHistory(histories.get(1), prevBidder, BalanceHistoryType.BID_UNLOCK, 12_000, 100_000, 0);
    }

    @Test
    void 사용_가능_포인트가_모자라면_BALANCE_INSUFFICIENT이고_아무것도_바뀌지_않는다() {
        User prevBidder = user(2L, 100_000);
        prevBidder.lock(12_000);
        User bidder = user(3L, 10_000);
        given(userRepository.findAllByIdInForUpdate(List.of(3L, 2L))).willReturn(List.of(prevBidder, bidder));

        assertThatThrownBy(() -> pointService.lockForBid(AUCTION_ID, 3L, 15_000, 2L, 12_000))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PointErrorCode.BALANCE_INSUFFICIENT);

        assertBalance(bidder, 10_000, 0);
        assertBalance(prevBidder, 88_000, 12_000);
        then(balanceHistoryRepository).should(never()).save(any());
    }

    @Test
    void 유저를_찾지_못하면_IllegalStateException() {
        given(userRepository.findAllByIdInForUpdate(List.of(3L))).willReturn(List.of());

        assertThatThrownBy(() -> pointService.lockForBid(AUCTION_ID, 3L, 12_000, null, 0))
                .isInstanceOf(IllegalStateException.class);
        then(balanceHistoryRepository).should(never()).save(any());
    }

    /** id와 사용 가능 포인트를 채운 테스트용 회원. id는 원래 DB가 채우는 값이라 직접 넣는다 */
    private static User user(Long id, long available) {
        User user = User.createUser("user" + id + "@test.com", "password", "user" + id, null);
        ReflectionTestUtils.setField(user, "id", id);
        user.charge(available);
        return user;
    }

    private static void assertBalance(User user, long available, long locked) {
        assertThat(user.getAvailableBalance()).isEqualTo(available);
        assertThat(user.getLockedBalance()).isEqualTo(locked);
    }

    private static void assertHistory(BalanceHistory history, User user, BalanceHistoryType type,
                                      long amount, long availableAfter, long lockedAfter) {
        assertThat(history.getUser()).isSameAs(user);
        assertThat(history.getType()).isEqualTo(type);
        assertThat(history.getAmount()).isEqualTo(amount);
        assertThat(history.getAvailableAfter()).isEqualTo(availableAfter);
        assertThat(history.getLockedAfter()).isEqualTo(lockedAfter);
        assertThat(history.getRefType()).isEqualTo(RefType.AUCTION);
        assertThat(history.getRefId()).isEqualTo(AUCTION_ID);
    }
}
