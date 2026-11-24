package com.team5.auction.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배송지 값 타입. User의 기본 배송지와 Trade의 배송지 스냅샷에 같이 씁니다.
 * setter 없는 불변 객체입니다. 다른 엔티티에 넣을 때는 반드시 {@link #copy()}로 복사합니다. (R-57)
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Address {

	@Column(length = 50)
	private String recipientName;

	@Column(length = 20)
	private String recipientPhone;

	@Column(length = 10)
	private String zipCode;

	@Column(length = 200)
	private String address1;

	@Column(length = 200)
	private String address2;

	public Address copy() {
		return new Address(recipientName, recipientPhone, zipCode, address1, address2);
	}
}
