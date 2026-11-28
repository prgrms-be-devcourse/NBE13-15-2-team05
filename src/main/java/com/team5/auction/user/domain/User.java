package com.team5.auction.user.domain;

import com.team5.auction.common.entity.Address;
import com.team5.auction.common.entity.BaseEntity;

import com.team5.auction.common.exception.BusinessException;
import com.team5.auction.point.exception.PointErrorCode;
import com.team5.auction.user.exception.UserErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원. 담당: 회원·공통
 * availableBalance, lockedBalance는 PointService만 바꿉니다. 잔액 변경 메서드는 포인트 담당이 작성합니다. (계약서 2.2)
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(nullable = false, unique = true, length = 30)
	private String nickname;

	@Column(nullable = false)
	private String password;

	@Column(length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false)
	private long availableBalance;

	@Column(nullable = false)
	private long lockedBalance;

	/** 기본 배송지 1개. 없으면 null. (R-65) */
	@Embedded
	private Address address;

	public boolean hasAddress() {
		return address != null;
	}

	private User(String email, String password, String nickname, String phone, Role role){
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.phone = phone;
		this.role = role;
		this.availableBalance = 0;
		this.lockedBalance = 0;
		this.address = null;
	}

	public static User createUser(String email, String password, String nickname, String phone){
		return new User(email, password, nickname, phone, Role.USER);
	}

	public void changeNickname(String nickname){
		this.nickname = nickname;
	}

	public void changePhone(String phone){
		this.phone = phone;
	}

	public void changeAddress(Address address){
		this.address = address.copy();
	}

	public void clearAddress(){
		if(lockedBalance > 0){
			throw new BusinessException(UserErrorCode.ADDRESS_REQUIRED_WHILE_LOCKED);
		}
		this.address = null;
	}

	/** 충전: available + (R-23) */
	public void charge(long amount) {
		requirePositive(amount);
		this.availableBalance += amount;
	}

	/** 입찰 홀딩: available → locked. 사용 가능 포인트가 모자라면 BALANCE_INSUFFICIENT (R-14, R-20) */
	public void lock(long amount) {
		requirePositive(amount);
		if (availableBalance < amount) {
			throw new BusinessException(PointErrorCode.BALANCE_INSUFFICIENT);
		}
		this.availableBalance -= amount;
		this.lockedBalance += amount;
	}

	/** 홀딩 해제·환불: locked → available (R-21, R-55, R-61) */
	public void unlock(long amount) {
		requirePositive(amount);
		requireLocked(amount);
		this.lockedBalance -= amount;
		this.availableBalance += amount;
	}

	/** 구매확정 차감: locked - (R-53) */
	public void deductLocked(long amount) {
		requirePositive(amount);
		requireLocked(amount);
		this.lockedBalance -= amount;
	}

	/** 판매자 정산: available + (R-53) */
	public void addAvailable(long amount) {
		requirePositive(amount);
		this.availableBalance += amount;
	}

	/** 금액이 0 이하면 호출한 코드의 버그 */
	private void requirePositive(long amount) {
		if (amount <= 0) {
			throw new IllegalArgumentException("금액은 0보다 커야 합니다. amount=" + amount);
		}
	}

	/** 묶인 포인트가 모자라면 코드 버그. 500 응답과 롤백으로 처리 (계약서 2.2) */
	private void requireLocked(long amount) {
		if (lockedBalance < amount) {
			throw new IllegalStateException(
					"묶인 포인트가 부족합니다. userId=" + id + ", locked=" + lockedBalance + ", amount=" + amount);
		}
	}
}
