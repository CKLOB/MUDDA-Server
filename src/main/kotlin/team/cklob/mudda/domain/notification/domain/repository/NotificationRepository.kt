package team.cklob.mudda.domain.notification.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.notification.domain.entity.Notification

interface NotificationRepository : JpaRepository<Notification, Long> {
	fun findByRecipientIdOrderByCreatedAtDesc(recipientId: Long): List<Notification>
	fun findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientId: Long): List<Notification>
}
