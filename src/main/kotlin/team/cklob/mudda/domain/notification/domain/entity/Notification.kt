package team.cklob.mudda.domain.notification.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.global.common.entity.BaseCreatedAtEntity

@Entity
@Table(name = "tbl_notification")
class Notification(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_id", nullable = false)
	val recipient: Member,

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 30)
	val notificationType: NotificationType,

	@Column(nullable = false, length = 255)
	val title: String,

	@Column(nullable = false, length = 255)
	val content: String,

	// A notification points at whatever the client should navigate to when it is tapped: a capsule, a
	// member, or a friend request. The pair is nullable together -- a notification that is purely
	// informational has nowhere to navigate.
	@Column(name = "target_id")
	val targetId: Long? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", length = 30)
	val targetType: NotificationTargetType? = null,

	@Column(name = "is_read", nullable = false)
	var isRead: Boolean = false,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseCreatedAtEntity()
