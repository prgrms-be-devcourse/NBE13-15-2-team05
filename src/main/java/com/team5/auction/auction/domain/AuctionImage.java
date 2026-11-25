package com.team5.auction.auction.domain;

import com.team5.auction.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 경매 이미지. URL 문자열만 저장합니다. (R-09) 담당: 경매 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionImage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "auction_id", nullable = false)
	private Auction auction;

	@Column(nullable = false, length = 2048)
	private String url;

	@Column(nullable = false)
	private int sortOrder;

	/** Auction.addImage()에서만 호출합니다. 경매 쪽 images 리스트에 추가하는 일은 addImage가 합니다. */
	static AuctionImage of(Auction auction, String url, int sortOrder) {
		AuctionImage image = new AuctionImage();
		image.auction = auction;
		image.url = url;
		image.sortOrder = sortOrder;
		return image;
	}
}
