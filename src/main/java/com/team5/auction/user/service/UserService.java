package com.team5.auction.user.service;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.dto.MeResponse;
import com.team5.auction.user.dto.PatchMeRequest;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MeResponse me(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return MeResponse.from(user);

    }

    @Transactional
    public MeResponse updateMe(Long userId, PatchMeRequest request){
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(()-> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 닉네임 중복 검사, null이 아니며 해당 유저의 현재 닉네임와 같지 않을 때만 다른 닉네임과 중복 검사 실시
        if(request.nickname() != null && !request.nickname().equals(user.getNickname())){
            if(userRepository.findByNickname(request.nickname()).isPresent()){
                throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
            }
            user.changeNickname(request.nickname());
        }
        if(request.phone() != null){
            user.changePhone(request.phone());
        }
        if(request.address() != null){
            user.changeAddress(request.address().toAddress());
        }

        if(request.shouldClearAddress()) {
            user.clearAddress();
        }

        try {
            // flush로 업데이트를 앞당겨 예외처리한다
            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // 수정이 동시에 들어왔을 때, 두 요청 중 하나가 DB 유니크 제약에 걸린 경우
            throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
        }

        return MeResponse.from(user);
    }
}
