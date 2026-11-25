package com.team5.auction.user.domain;

import com.team5.auction.common.entity.Address;
import com.team5.auction.common.entity.BaseEntity;

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
}
