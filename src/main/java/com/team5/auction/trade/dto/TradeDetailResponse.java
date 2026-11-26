package com.team5.auction.trade.dto;

import com.team5.auction.common.entity.Address;
import com.team5.auction.trade.domain.CancelReason;
import com.team5.auction.trade.domain.Trade;
import com.team5.auction.trade.domain.TradeStatus;
import com.team5.auction.trade.domain.TradeType;
import com.team5.auction.user.domain.User;

import java.time.LocalDateTime;

/**
 * 거래 상세 응답. 목록 필드에 상세 전용 필드를 더한 형태입니다.
 * 거래 당사자(구매자, 판매자)만 조회할 수 있습니다.
 */
public record TradeDetailResponse(
        // 목록과 같은 필드
        Long tradeId,
        Long auctionId,
        String auctionTitle,
        TradeType type,
        Long price,
        TradeStatus status,
        LocalDateTime tradedAt,
        /** 송장 입력 기한. 지나면 자동 취소됩니다. (R-56) */
        LocalDateTime shipDeadlineAt,
        /** 자동 구매확정 예정 시각. 송장 입력 전에는 null입니다. (R-52) */
        LocalDateTime autoConfirmAt,
        // 상세 전용 필드
        /** 거래 상대방. 내가 구매자면 판매자, 판매자면 구매자입니다. (R-50) */
        Counterpart counterpart,
        /** 거래 성립 시점에 복사해 둔 배송지입니다. (R-57) */
        ShippingAddress shippingAddress,
        String trackingNumber,
        /** 수수료. 판매자에게만 내려주고 구매자에게는 null입니다. (R-54) */
        Long feeAmount,
        /** 정산금(price - feeAmount). 판매자에게만 내려주고 구매자에게는 null입니다. (R-54) */
        Long settleAmount,
        LocalDateTime shippedAt,
        LocalDateTime confirmedAt,
        LocalDateTime canceledAt,
        CancelReason cancelReason
) {
    /**
     * Trade 엔티티를 상세 응답으로 변환합니다.
     *
     * @param trade       조회한 거래
     * @param isSeller    요청자가 판매자인지 여부. false면 수수료와 정산금을 숨깁니다.
     */
    public static TradeDetailResponse of(Trade trade, boolean isSeller) {
        User counterpart = isSeller ? trade.getBuyer() : trade.getSeller();

        return new TradeDetailResponse(
                trade.getId(),
                trade.getAuction().getId(),
                trade.getAuction().getTitle(),
                trade.getType(),
                trade.getPrice(),
                trade.getStatus(),
                trade.getTradedAt(),
                trade.getShipDeadlineAt(),
                trade.getAutoConfirmAt(),
                Counterpart.from(counterpart),
                ShippingAddress.from(trade.getShippingAddress()),
                trade.getTrackingNumber(),
                isSeller ? trade.getFeeAmount() : null ,
                isSeller ? trade.getSettleAmount() : null,
                trade.getShippedAt(),
                trade.getConfirmedAt(),
                trade.getCanceledAt(),
                trade.getCancelReason()
        );
    }

    /** 거래 상대방의 연락처 정보. (R-50) */
    public record Counterpart(String nickname, String phone) {
        public static Counterpart from (User user) {
            return new Counterpart(user.getNickname(), user.getPhone());
        }
    }

    /** 거래에 스냅샷된 배송지의 응답용 DTO. (R-57) */
    public record ShippingAddress(
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address1,
            String address2
    ) {
        /** Address 값 타입을 응답용으로 변환합니다. 배송지가 없으면 null을 반환합니다. */
        public static ShippingAddress from(Address address) {
            if (address == null) {
                return null;
            }
            return new ShippingAddress(
                    address.getRecipientName(),
                    address.getRecipientPhone(),
                    address.getZipCode(),
                    address.getAddress1(),
                    address.getAddress2()
            );
        }
    }
}


