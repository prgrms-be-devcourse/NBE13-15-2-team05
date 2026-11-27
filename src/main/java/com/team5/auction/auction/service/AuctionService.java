package com.team5.auction.auction.service;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.domain.AuctionStatus;
import com.team5.auction.auction.dto.AuctionCreateRequest;
import com.team5.auction.auction.dto.AuctionListResponse;
import com.team5.auction.auction.exception.AuctionErrorCode;
import com.team5.auction.auction.repository.AuctionRepository;
import com.team5.auction.category.domain.Category;
import com.team5.auction.category.exception.CategoryErrorCode;
import com.team5.auction.category.repository.CategoryRepository;
import com.team5.auction.common.config.AuctionPolicyProperties;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    // 등록 규칙 (docs/프로젝트_기술서.md R-02, R-08, R-09)
    private static final int MIN_DURATION_DAYS = 1;
    private static final int MAX_DURATION_DAYS = 14;
    private static final long MIN_PRICE = 1_000L;
    private static final long PRICE_UNIT = 100L;
    private static final int MIN_IMAGE_COUNT = 1;
    private static final int MAX_IMAGE_COUNT = 10;

    private final Clock clock;

    // 마감 연장 정책 (R-05). application.yml의 auction.extension 값을 앱 시작 시 바인딩해 둔 것.
    // 등록 시점 값을 경매에 복사해 두므로, 나중에 yml을 바꿔도 이미 등록된 경매는 영향받지 않는다.
    private final AuctionPolicyProperties policyProperties;

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;


    @Transactional
    public Auction create(Long userId, AuctionCreateRequest request) {
        // 요청값만으로 판단할 수 있는 규칙은 DB 조회 전에 검사한다.
        validatePeriod(request.durationDays());
        validatePrices(request.startPrice(), request.buyNowPrice());
        validateImages(request.imageUrls());

        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));

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

        List<String> imageUrls = request.imageUrls();   // validateImages에서 null·빈 리스트는 이미 걸러짐
        for (int i = 0; i < imageUrls.size(); i++) {
            auction.addImage(imageUrls.get(i), i);
        }

        return auctionRepository.save(auction);
    }

    //경매 목록 조회
    public Page<AuctionListResponse> getAuctions(Long categoryId, AuctionStatus status, Pageable pageable) {
        // 카테고리 이름·대표 이미지를 한 쿼리로 DTO에 담아 오므로 N+1이 생기지 않는다.
        return auctionRepository.findAuctionList(categoryId, status, pageable);
    }


    // 경매 기간 검증
    private void validatePeriod(int durationDays) {
        if (durationDays < MIN_DURATION_DAYS || durationDays > MAX_DURATION_DAYS) {
            throw new BusinessException(AuctionErrorCode.AUCTION_PERIOD_INVALID);
        }
    }

    // 가격 검증 메서드
    private void validatePrices(long startPrice, Long buyNowPrice) {
        validatePriceUnit(startPrice);
        if (buyNowPrice == null) {          // 즉시구매가는 선택값
            return;
        }
        validatePriceUnit(buyNowPrice);     // 최소 1,000원
        if (startPrice >= buyNowPrice) {    //즉시구매가가 시작가보다 큰가
            throw new BusinessException(AuctionErrorCode.BUY_NOW_PRICE_INVALID);
        }
    }

    // 100원 단위, 최소 1,000원 (R-08)
    private void validatePriceUnit(long price) {
        if (price < MIN_PRICE || price % PRICE_UNIT != 0) {
            throw new BusinessException(AuctionErrorCode.PRICE_UNIT_INVALID);
        }
    }

    // 이미지 1장 이상 10장 이하 (R-09)
    private void validateImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.size() < MIN_IMAGE_COUNT || imageUrls.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(AuctionErrorCode.IMAGE_COUNT_INVALID);
        }
    }
}