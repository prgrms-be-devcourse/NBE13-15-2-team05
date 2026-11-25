package com.team5.auction.bid.service;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team5.auction.auction.exception.AuctionErrorCode;
import com.team5.auction.auction.repository.AuctionRepository;

import com.team5.auction.bid.dto.AdminBidResponse;
import com.team5.auction.bid.dto.BidHistoryResponse;
import com.team5.auction.bid.dto.BidResultResponse;
import com.team5.auction.bid.dto.MyBidResponse;
import com.team5.auction.bid.repository.BidRepository;
import com.team5.auction.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/** 입찰 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

	private final BidRepository bidRepository;
	private final AuctionRepository auctionRepository;

	/** TODO(입찰): 입찰 트랜잭션 8단계 (기술서 9.1). 구현 시 @Transactional 필수 (클래스 기본이 readOnly) */
	public BidResultResponse placeBid(Long auctionId, Long bidderId, long amount) {
		throw new UnsupportedOperationException("TODO(입찰)");
	}

	/** TODO(입찰): 즉시구매가로 입찰한 것과 똑같이 처리 (R-44). 구현 시 @Transactional 필수 */
	public BidResultResponse buyNow(Long auctionId, Long bidderId) {
		throw new UnsupportedOperationException("TODO(입찰)");
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

	private void validateAuctionExists(Long auctionId) {
		if (!auctionRepository.existsById(auctionId)) {
			throw new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND);
		}
	}
}