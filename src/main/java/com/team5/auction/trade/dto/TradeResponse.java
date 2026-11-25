package com.team5.auction.trade.dto;

import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeStatus;
import com.team5.auction.trade.domain.TradeType;
import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 거래 목록 응답. 내가 구매자 또는 판매자인 거래 목록에서 한 건을 나타냅니다.
 * 상세 정보(상대방 연락처, 배송지, 정산 금액 등)는 {@link TradeDetailResponse}에서 내려줍니다.
 */
public record TradeResponse (
        Long tradeId,
        Long auctionId,
        String auctionTitle,
        /** 거래 유형. 낙찰인지 즉시구매인지 구분합니다. */
        TradeType type,
        /** 거래 금액(낙찰가 또는 즉시구매가)입니다. */
        Long price,
        TradeStatus status,
        LocalDateTime tradedAt,
        /** 송장 입력 기한. 지나면 자동 취소됩니다. (R-56) */
        LocalDateTime shipDeadlineAt,
        /** 자동 구매확정 예정 시각. 송장 입력 전에는 null입니다. (R-52) */
        LocalDateTime autoConfirmAt
){
    public static TradeResponse from (Trade trade) {
        return new TradeResponse(trade.getId(),
                trade.getAuction().getId(),
                trade.getAuction().getTitle(),
                trade.getType(),
                trade.getPrice(),
                trade.getStatus(),
                trade.getTradedAt(),
                trade.getShipDeadlineAt(),
                trade.getAutoConfirmAt()
        );
    }
}
