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
		// Notification text is assembled from user-supplied values (nicknames, capsule names), so its length
		// is not bounded by construction. Overflowing the column would throw on flush and roll back the
		// caller's whole transaction -- failing a capsule creation because its banner text was too long.
		// Callers keep their messages short; this is the backstop that makes that impossible to get wrong.
		val safeTitle = title.truncateForColumn()
		val safeContent = content.truncateForColumn()
		notificationRepository.save(Notification(recipient, type, safeTitle, safeContent, targetId, targetType))
		val recipientId = requireNotNull(recipient.id)
		// Deferred to after commit so a rolled-back capsule open doesn't push a banner for a notification
		// row that no longer exists. Deliberately a separate bean rather than a private method: @Async only
		// takes effect through the Spring proxy, which a self-invocation would bypass.
		afterCommit { pushSender.send(recipientId, safeTitle, safeContent) }
	}

	private fun String.truncateForColumn() =
		if (length <= MAX_TEXT_LENGTH) this else take(MAX_TEXT_LENGTH - 1) + "…"

	private companion object {
		// Matches the VARCHAR(255) width of tbl_notification.title and .content.
		const val MAX_TEXT_LENGTH = 255
	}
}
