package com.team5.auction.user.service;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.user.domain.Role;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.dto.AddressDto;
import com.team5.auction.user.dto.MeResponse;
import com.team5.auction.user.dto.PatchMeRequest;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.repository.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private final AddressDto address =
            new AddressDto("홍길동", "010-2222-3333", "06236", "서울시 강남구 테헤란로 1", "101호");

    /** DB에 저장된 회원을 흉내. */
    private User storedUser() {
        User user = User.createUser("user@test.com", "{bcrypt}encoded", "경매왕", "010-1234-5678");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private PatchMeRequest request(String nickname, String phone, AddressDto address, Boolean clearAddress) {
        return new PatchMeRequest(nickname, phone, address, clearAddress);
    }

    // ===== me() : 락 없이 findById =====

    @Test
    void 배송지가_있는_회원의_정보를_조회한다() {
        User user = storedUser();
        user.changeAddress(address.toAddress());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        MeResponse response = userService.me(USER_ID);

        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.nickname()).isEqualTo("경매왕");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.availableBalance()).isZero();
        assertThat(response.lockedBalance()).isZero();
        assertThat(response.address()).isEqualTo(address);
    }

    @Test
    void 배송지가_없는_회원을_조회하면_address는_null이다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(storedUser()));

        MeResponse response = userService.me(USER_ID);

        assertThat(response.address()).isNull();
    }

    @Test
    void 회원이_없으면_조회는_USER_NOT_FOUND() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> userService.me(USER_ID), UserErrorCode.USER_NOT_FOUND);
    }

    // ===== updateMe() : 쓰기 락 findByIdForUpdate =====

    @Test
    void 수정은_쓰기_락으로_회원을_조회한다() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(storedUser()));

        userService.updateMe(USER_ID, request(null, "010-5555-6666", null, null));

        then(userRepository).should().findByIdForUpdate(USER_ID);
        then(userRepository).should(never()).findById(any());
    }

    @Test
    void 회원이_없으면_수정은_USER_NOT_FOUND() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

        assertErrorCode(() -> userService.updateMe(USER_ID, request("새닉네임", null, null, null)),
                UserErrorCode.USER_NOT_FOUND);
    }

    // ----- 닉네임 -----

    @Test
    void 다른_닉네임으로_바꾸면_중복_검사_후_변경된다() {
        User user = storedUser();
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.findByNickname("새닉네임")).willReturn(Optional.empty());

        MeResponse response = userService.updateMe(USER_ID, request("새닉네임", null, null, null));

        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(user.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    void 다른_회원이_쓰는_닉네임이면_NICKNAME_DUPLICATED_닉네임은_그대로다() {
        User user = storedUser();
        User other = User.createUser("other@test.com", "{bcrypt}x", "새닉네임", "010-9999-9999");
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(userRepository.findByNickname("새닉네임")).willReturn(Optional.of(other));

        assertErrorCode(() -> userService.updateMe(USER_ID, request("새닉네임", null, null, null)),
                UserErrorCode.NICKNAME_DUPLICATED);
        assertThat(user.getNickname()).isEqualTo("경매왕");
    }

    @Test
    void 지금과_같은_닉네임을_보내면_중복_검사를_하지_않는다() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(storedUser()));

        MeResponse response = userService.updateMe(USER_ID, request("경매왕", null, null, null));

        assertThat(response.nickname()).isEqualTo("경매왕");
        then(userRepository).should(never()).findByNickname(any());
    }

    @Test
    void 동시_수정으로_유니크_제약에_걸리면_NICKNAME_DUPLICATED() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(storedUser()));
        given(userRepository.findByNickname("새닉네임")).willReturn(Optional.empty());
        willThrow(new DataIntegrityViolationException("duplicate")).given(userRepository).flush();

        assertErrorCode(() -> userService.updateMe(USER_ID, request("새닉네임", null, null, null)),
                UserErrorCode.NICKNAME_DUPLICATED);
    }

    // ----- 보낸 필드만 -----

    @Test
    void 보내지_않은_필드는_바뀌지_않는다() {
        User user = storedUser();
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        userService.updateMe(USER_ID, request(null, "010-5555-6666", null, null));

        assertThat(user.getPhone()).isEqualTo("010-5555-6666");
        assertThat(user.getNickname()).isEqualTo("경매왕");
        assertThat(user.hasAddress()).isFalse();
    }

    @Test
    void clearAddress를_보내지_않으면_배송지는_그대로다() {
        User user = storedUser();
        user.changeAddress(address.toAddress());
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        userService.updateMe(USER_ID, request(null, "010-5555-6666", null, null));

        assertThat(user.hasAddress()).isTrue();
    }

    // ----- 배송지 -----

    @Test
    void 배송지를_보내면_등록된다() {
        User user = storedUser();
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        MeResponse response = userService.updateMe(USER_ID, request(null, null, address, null));

        assertThat(response.address()).isEqualTo(address);
        assertThat(user.getAddress().getZipCode()).isEqualTo("06236");
    }

    @Test
    void 묶인_포인트가_없으면_배송지를_삭제할_수_있다() {
        User user = storedUser();
        user.changeAddress(address.toAddress());
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        MeResponse response = userService.updateMe(USER_ID, request(null, null, null, true));

        assertThat(response.address()).isNull();
        assertThat(user.hasAddress()).isFalse();
    }

    @Test
    void 묶인_포인트가_있으면_배송지_삭제는_ADDRESS_REQUIRED_WHILE_LOCKED() {
        User user = storedUser();
        user.changeAddress(address.toAddress());
        ReflectionTestUtils.setField(user, "lockedBalance", 5_000L); // 입찰로 포인트가 묶인 상태
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

        assertErrorCode(() -> userService.updateMe(USER_ID, request(null, null, null, true)),
                UserErrorCode.ADDRESS_REQUIRED_WHILE_LOCKED);
        assertThat(user.hasAddress()).isTrue();
    }

    private void assertErrorCode(ThrowingCallable call, UserErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
