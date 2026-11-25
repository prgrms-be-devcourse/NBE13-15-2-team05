package com.team5.auction.user.service;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.dto.SignupRequest;
import com.team5.auction.user.dto.SignupResponse;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
