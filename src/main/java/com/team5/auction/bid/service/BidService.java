package com.team5.auction.bid.service;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.team5.auction.bid.dto.AdminBidResponse;
import com.team5.auction.bid.dto.BidHistoryResponse;
import com.team5.auction.bid.dto.BidResultResponse;
import com.team5.auction.bid.dto.MyBidResponse;

import lombok.RequiredArgsConstructor;

/** 입찰 서비스. 지금은 컨트롤러 껍데기용 자리만 있습니다. */
@Service
@RequiredArgsConstructor
public class BidService {

	/** TODO(입찰): 입찰 트랜잭션 8단계 (기술서 9.1) */
	public BidResultResponse placeBid(Long auctionId, Long bidderId, long amount) {
		throw new UnsupportedOperationException("TODO(입찰)");
	}

	/** TODO(입찰): 즉시구매가로 입찰한 것과 똑같이 처리 (R-44) */
	public BidResultResponse buyNow(Long auctionId, Long bidderId) {
		throw new UnsupportedOperationException("TODO(입찰)");
	}

	/** TODO(입찰): 금액·시각만 공개, 본인 입찰 표시 (R-18) */
	public Page<BidHistoryResponse> getBids(Long auctionId, @Nullable Long viewerId, Pageable pageable) {
		throw new UnsupportedOperationException("TODO(입찰)");
	}

	/** TODO(입찰): 내 입찰 목록, 최신순 */
	public Page<MyBidResponse> getMyBids(Long bidderId, Pageable pageable) {
		throw new UnsupportedOperationException("TODO(입찰)");
	}

	/** TODO(입찰): 운영자용, 실제 입찰자 포함 */
	public Page<AdminBidResponse> getBidsForAdmin(Long auctionId, Pageable pageable) {
		throw new UnsupportedOperationException("TODO(입찰)");
	}
}