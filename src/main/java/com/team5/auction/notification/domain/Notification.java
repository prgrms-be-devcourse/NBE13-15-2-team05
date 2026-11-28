package com.team5.auction.notification.domain;

import java.time.LocalDateTime;

import com.team5.auction.common.entity.BaseEntity;
import com.team5.auction.common.entity.RefType;
import com.team5.auction.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 서비스 내 알림 (R-63). 담당: 입찰(팀장) */
@Getter
@Entity
@Table(indexes = @Index(name = "idx_notification_user", columnList = "user_id, id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationType type;

	@Column(nullable = false, length = 255)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private RefType refType;

	private Long refId;

	/** null이면 안 읽음 */
	private LocalDateTime readAt;

	private Notification(User user, NotificationType type, String message, RefType refType, Long refId) {
		this.user = user;
		this.type = type;
		this.message = message;
		this.refType = refType;
		this.refId = refId;
	}

	public static Notification create(User user, NotificationType type, String message, RefType refType, Long refId) {
		return new Notification(user, type, message, refType, refId);
	}
}
