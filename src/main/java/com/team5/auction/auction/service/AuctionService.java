package com.team5.auction.auction.service;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.dto.AuctionCreateRequest;
import com.team5.auction.auction.repository.AuctionRepository;
import com.team5.auction.category.domain.Category;
import com.team5.auction.category.repository.CategoryRepository;
import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final Clock clock;

    // 마감 연장 정책 (R-05). application.yml의 auction.extension 값을 앱 시작 시 바인딩해 둔 것.
    // 등록 시점 값을 경매에 복사해 두므로, 나중에 yml을 바꿔도 이미 등록된 경매는 영향받지 않는다.
    private final AuctionPolicyProperties policyProperties;

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;


    @Transactional
    public Auction create(Long userId, AuctionCreateRequest request) {
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("CATEGORY_NOT_FOUND"));


        LocalDateTime startAt = LocalDateTime.now(clock);
        AuctionPolicyProperties.Extension extension = policyProperties.extension();

        Auction auction = Auction.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .startPrice(request.startPrice())
                .buyNowPrice(request.buyNowPrice())
                .startAt(startAt)
                .durationDays(request.durationDays())
                .extendEnabled(extension.enabled())
                .extendTriggerMinutes(extension.triggerMinutes())
                .extendMinutes(extension.extendMinutes())
                .build();

        List<String> imageUrls = request.imageUrls() == null ? List.of() : request.imageUrls();
        for (int i = 0; i < imageUrls.size(); i++) {
            auction.addImage(imageUrls.get(i), i);
        }

        // images는 cascade = ALL이라 경매를 저장할 때 함께 저장된다.
        return auctionRepository.save(auction);
    }
}