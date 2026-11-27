package com.team5.auction.user.service;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.common.security.JwtTokenProvider;
import com.team5.auction.common.security.LoginUser;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.dto.LoginRequest;
import com.team5.auction.user.dto.LoginResponse;
import com.team5.auction.user.dto.SignupRequest;
import com.team5.auction.user.dto.SignupResponse;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request){

        // 이메일 중복 검사
        if(userRepository.findByEmail(request.email()).isPresent()){
            throw new BusinessException(UserErrorCode.EMAIL_DUPLICATED);
        }

        // 닉네임 중복 검사
        if(userRepository.findByNickname(request.nickname()).isPresent()){
            throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
        }

        User user = User.createUser(request.email(), passwordEncoder.encode(request.password()),
                request.nickname(), request.phone());

        try {
            return SignupResponse.from(userRepository.save(user));
        } catch (DataIntegrityViolationException ex) {
            // 가입이 동시에 들어왔을 때, 두 요청 중 하나가 DB 유니크 제약에 걸린 경우
            throw new BusinessException(UserErrorCode.EMAIL_DUPLICATED, "이미 사용 중인 이메일 또는 닉네임입니다.");
        }

    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request){

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(UserErrorCode.LOGIN_FAILED);
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(()-> new BusinessException(UserErrorCode.LOGIN_FAILED));

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new BusinessException(UserErrorCode.LOGIN_FAILED);
        }
        LoginUser loginUser = new LoginUser(user.getId(), user.getRole());

        return LoginResponse.of(jwtTokenProvider.createAccessToken(loginUser), jwtTokenProvider.getAccessSeconds());
    }
}
