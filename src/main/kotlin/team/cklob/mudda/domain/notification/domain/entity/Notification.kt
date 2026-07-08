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
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_notification")
class Notification(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_id", nullable = false)
	val recipient: Member,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_capsule_id")
	val timeCapsule: TimeCapsule? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 30)
	val notificationType: NotificationType,

	@Column(nullable = false, length = 255)
	val title: String,

	@Column(nullable = false, length = 255)
	val body: String,

	@Column(name = "is_read", nullable = false)
	val isRead: Boolean = false,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) {
	@Column(name = "created_at", nullable = false)
	var createdAt: LocalDateTime = LocalDateTime.now()
		protected set

	protected constructor() : this(Member("", "", "", null, null, ""), null, NotificationType.FRIEND_REQUESTED, "", "", false)

	@PrePersist
	fun prePersist() {
		createdAt = LocalDateTime.now()
	}
}
