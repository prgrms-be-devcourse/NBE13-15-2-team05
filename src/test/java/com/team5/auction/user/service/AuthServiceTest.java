package com.team5.auction.user.service;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.security.JwtTokenProvider;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.user.domain.Role;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.dto.LoginRequest;
import com.team5.auction.user.dto.LoginResponse;
import com.team5.auction.user.dto.SignupRequest;
import com.team5.auction.user.dto.SignupResponse;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private final SignupRequest request =
            new SignupRequest("user@test.com", "password123", "경매왕", "010-1234-5678");

    /** 이미 가입된 회원(중복 검사에서 "찾았음"을 표현하는 용도). */
    private final User existing =
            User.createUser("user@test.com", "{bcrypt}old", "경매왕", "010-0000-0000");

    @Test
    void 가입하면_USER_잔액0_배송지없음으로_암호화된_비밀번호와_함께_저장된다() {
        given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());
        given(userRepository.findByNickname(request.nickname())).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("{bcrypt}encoded");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L); // DB가 id를 붙여주는 것을 흉내
            return saved;
        });

        SignupResponse response = authService.signup(request);

        assertThat(response.userId()).isEqualTo(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@test.com");
        assertThat(saved.getPassword()).isEqualTo("{bcrypt}encoded").isNotEqualTo("password123");
        assertThat(saved.getNickname()).isEqualTo("경매왕");
        assertThat(saved.getPhone()).isEqualTo("010-1234-5678");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getAvailableBalance()).isZero();
        assertThat(saved.getLockedBalance()).isZero();
        assertThat(saved.hasAddress()).isFalse();
    }

    @Test
    void 이메일이_중복이면_EMAIL_DUPLICATED_저장하지_않는다() {
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(existing));

        assertErrorCode(UserErrorCode.EMAIL_DUPLICATED);
        then(userRepository).should(never()).save(any());
        then(passwordEncoder).should(never()).encode(any());
    }

    @Test
    void 닉네임이_중복이면_NICKNAME_DUPLICATED_저장하지_않는다() {
        given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());
        given(userRepository.findByNickname(request.nickname())).willReturn(Optional.of(existing));

        assertErrorCode(UserErrorCode.NICKNAME_DUPLICATED);
        then(userRepository).should(never()).save(any());
    }

    @Test
    void 동시_가입으로_유니크_제약에_걸리면_500이_아닌_중복_에러로_바꾼다() {
        given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());
        given(userRepository.findByNickname(request.nickname())).willReturn(Optional.empty());
        given(passwordEncoder.encode(any())).willReturn("{bcrypt}encoded");
        given(userRepository.save(any(User.class))).willThrow(new DataIntegrityViolationException("duplicate"));

        assertErrorCode(UserErrorCode.EMAIL_DUPLICATED);
    }

    // ===== 로그인 =====

    private final LoginRequest loginRequest = new LoginRequest("user@test.com", "password123");

    /** DB에 저장된 회원을 흉내. id와 role은 DB 값이라고 가정하고 직접 넣습니다. */
    private User storedUser(Role role) {
        User user = User.createUser("user@test.com", "{bcrypt}encoded", "경매왕", "010-1234-5678");
        ReflectionTestUtils.setField(user, "id", 7L);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }

    @Test
    void 로그인에_성공하면_DB의_id와_role로_토큰을_발급하고_Bearer와_만료시간을_응답한다() {
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(storedUser(Role.ADMIN)));
        given(passwordEncoder.matches("password123", "{bcrypt}encoded")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(new LoginUser(7L, Role.ADMIN))).willReturn("access-token");
        given(jwtTokenProvider.getAccessSeconds()).willReturn(3600L);

        LoginResponse response = authService.login(loginRequest);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void 이메일이_없으면_LOGIN_FAILED_토큰을_발급하지_않는다() {
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.empty());

        assertLoginFailed(loginRequest);
        then(passwordEncoder).should(never()).matches(any(), any());
        then(jwtTokenProvider).should(never()).createAccessToken(any());
    }

    @Test
    void 비밀번호가_틀리면_LOGIN_FAILED_토큰을_발급하지_않는다() {
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(storedUser(Role.USER)));
        given(passwordEncoder.matches("password123", "{bcrypt}encoded")).willReturn(false);

        assertLoginFailed(loginRequest);
        then(jwtTokenProvider).should(never()).createAccessToken(any());
    }

    @Test
    void 비밀번호가_72바이트를_넘으면_조회하지_않고_LOGIN_FAILED() {
        String longPassword = "가".repeat(25); // 한글 25자 = UTF-8 75바이트

        assertLoginFailed(new LoginRequest("user@test.com", longPassword));
        then(userRepository).should(never()).findByEmail(any());
        then(passwordEncoder).should(never()).matches(any(), any());
    }

    private void assertLoginFailed(LoginRequest request) {
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(UserErrorCode.LOGIN_FAILED);
    }

    private void assertErrorCode(UserErrorCode expected) {
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(expected);
    }
}
