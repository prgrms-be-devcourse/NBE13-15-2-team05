package com.team5.auction.bid.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team5.auction.auction.domain.Auction;
import com.team5.auction.auction.exception.AuctionErrorCode;
import com.team5.auction.auction.repository.AuctionRepository;

import com.team5.auction.bid.dto.AdminBidResponse;
import com.team5.auction.bid.dto.BidHistoryResponse;
import com.team5.auction.bid.dto.BidResultResponse;
import com.team5.auction.bid.dto.MyBidResponse;
import com.team5.auction.bid.domain.Bid;
import com.team5.auction.bid.event.OutbidEvent;
import com.team5.auction.bid.exception.BidErrorCode;
import com.team5.auction.bid.repository.BidRepository;
import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.point.service.PointService;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.trade.service.TradeService;
import com.team5.auction.user.domain.User;
import com.team5.auction.user.exception.UserErrorCode;
import com.team5.auction.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** 입찰 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

	private final BidRepository bidRepository;
	private final AuctionRepository auctionRepository;
	private final UserRepository userRepository;
	private final BidUnitPolicy bidUnitPolicy;
	private final PointService pointService;
	private final TradeService tradeService;
	private final Clock clock;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 입찰 (기술서 9.1). 즉시구매가 이상이면 즉시구매가로 낙찰 처리한다 (R-43, R-40).
	 * version 충돌(R-16)은 커밋 시점에 나므로 여기서 잡지 않는다.
	 */
	@Transactional
	public BidResultResponse placeBid(Long auctionId, Long bidderId, long amount) {
		LocalDateTime now = LocalDateTime.now(clock);

		// 1. 마감 여부 (R-17)
		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
		if (!auction.isOpenAt(now)) {
			throw new BusinessException(BidErrorCode.AUCTION_CLOSED);
		}

		// 2. 입찰 자격 (R-10, R-11, R-19)
		User bidder = userRepository.findById(bidderId)
			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		validateBidder(auction, bidder);

		// 3. 금액 (R-12, R-13). 즉시구매가 이상이면 즉시구매가로 처리 (R-43)
		boolean buyNow = auction.reachesBuyNow(amount);
		long price = buyNow ? auction.getBuyNowPrice() : amount;
		if (!buyNow && amount < auction.minimumBid(bidUnitPolicy.unitFor(auction.getCurrentPrice()))) {
			throw new BusinessException(BidErrorCode.BID_TOO_LOW);
		}

		// 4. 포인트 홀딩 + 이전 최고 입찰자 해제 (R-14, R-20, R-21)
		User prevBidder = auction.getHighestBidder();
		long prevAmount = auction.getCurrentPrice();
		pointService.lockForBid(auctionId, bidderId, price,
			prevBidder == null ? null : prevBidder.getId(),
			prevBidder == null ? 0 : prevAmount);

		// 5. 경매 반영 + 마감 연장 (R-30)
		boolean extended = auction.applyBid(bidder, price, now);
		Bid bid = bidRepository.save(Bid.create(auction, bidder, price, now));

		// 6. 즉시구매가 도달 시 낙찰·거래 생성 (R-40, R-42)
		Long tradeId = null;
		if (buyNow) {
			auction.markBoughtNow();
			Trade trade = tradeService.createTrade(auction, bidder, TradeType.BUY_NOW, price);
			tradeId = trade.getId();
		}

		// 7. 커밋은 메서드가 끝날 때 @Transactional이 한다.
		//    version 충돌은 커밋 시점에 나므로 여기서 잡지 않고 BidExceptionHandler가 409(BID_CONFLICT)로 바꾼다 (R-16)

		// 8. 이전 최고 입찰자에게 OUTBID 알림 (즉시구매 포함).
		//    트랜잭션 안에서 발행하고, 알림 쪽이 커밋 후(AFTER_COMMIT)에 받으므로 롤백되면 알림도 가지 않는다
		if (prevBidder != null)
			eventPublisher.publishEvent(new OutbidEvent(auctionId, auction.getTitle(), prevBidder.getId(), price));

		return BidResultResponse.of(bid, auction, extended, tradeId);
	}

	/**
	 * 즉시구매. 즉시구매가로 입찰한 것과 똑같이 처리한다 (R-44).
	 * 마감·자격 검증, 포인트, 거래 생성, OUTBID 알림은 모두 placeBid가 한다.
	 *
	 * <p>같은 클래스 안 호출이라 placeBid의 @Transactional이 적용되지 않으므로 여기에 붙인다.
	 * 경매는 같은 영속성 컨텍스트에서 다시 조회되므로 쿼리가 추가로 나가지 않는다.
	 */
	@Transactional
	public BidResultResponse buyNow(Long auctionId, Long bidderId) {
		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

		// 즉시구매가가 없거나 현재가가 이미 즉시구매가에 도달했으면 불가 (R-03, R-45)
		if (!auction.canBuyNow()) {
			throw new BusinessException(BidErrorCode.BUY_NOW_NOT_AVAILABLE);
		}

		return placeBid(auctionId, bidderId, auction.getBuyNowPrice());
	}

	/** 경매의 입찰 내역 (R-18) */
	public Page<BidHistoryResponse> getBids(Long auctionId, @Nullable Long viewerId, Pageable pageable) {
		validateAuctionExists(auctionId);
		return bidRepository.findByAuctionId(auctionId, pageable)
			.map(bid -> BidHistoryResponse.of(bid, viewerId));
	}

	/** 내 입찰 목록 */
	public Page<MyBidResponse> getMyBids(Long bidderId, Pageable pageable) {
		return bidRepository.findWithAuctionByBidderId(bidderId, pageable)
			.map(bid -> MyBidResponse.of(bid, bidderId));
	}

	/** 운영자용 입찰 내역 (R-18) */
	public Page<AdminBidResponse> getBidsForAdmin(Long auctionId, Pageable pageable) {
		validateAuctionExists(auctionId);
		return bidRepository.findWithBidderByAuctionId(auctionId, pageable)
			.map(AdminBidResponse::from);
	}

	private void validateBidder(Auction auction, User bidder) {
		if (auction.isSeller(bidder.getId())) {
			throw new BusinessException(BidErrorCode.SELF_BID_NOT_ALLOWED);
		}
		if (auction.isHighestBidder(bidder.getId())) {
			throw new BusinessException(BidErrorCode.ALREADY_HIGHEST_BIDDER);
		}
		if (!bidder.hasAddress()) {
			throw new BusinessException(BidErrorCode.ADDRESS_REQUIRED);
		}
	}

	private void validateAuctionExists(Long auctionId) {
		if (!auctionRepository.existsById(auctionId)) {
			throw new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND);
		}
	}
}