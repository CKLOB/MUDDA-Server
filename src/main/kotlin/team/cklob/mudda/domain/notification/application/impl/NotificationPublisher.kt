package team.cklob.mudda.domain.notification.application.impl

import org.springframework.stereotype.Component
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.notification.domain.entity.Notification
import team.cklob.mudda.domain.notification.domain.repository.NotificationRepository
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.global.util.afterCommit

// Single entry point the other domains call to raise a notification: persists the row, then hands the
// push off to DevicePushSender. Kept out of the caller's own service so capsule/friend logic never
// touches FCM directly.
@Component
class NotificationPublisher(
	private val notificationRepository: NotificationRepository,
	private val pushSender: DevicePushSender,
) {
	fun publish(
		recipient: Member,
		type: NotificationType,
		title: String,
		content: String,
		targetId: Long? = null,
		targetType: NotificationTargetType? = null,
	) {
		notificationRepository.save(Notification(recipient, type, title, content, targetId, targetType))
		val recipientId = requireNotNull(recipient.id)
		// Deferred to after commit so a rolled-back capsule open doesn't push a banner for a notification
		// row that no longer exists. Deliberately a separate bean rather than a private method: @Async only
		// takes effect through the Spring proxy, which a self-invocation would bypass.
		afterCommit { pushSender.send(recipientId, title, content) }
	}
}
