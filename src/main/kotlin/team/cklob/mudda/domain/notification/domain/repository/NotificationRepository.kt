package team.cklob.mudda.domain.notification.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.notification.domain.entity.Notification

interface NotificationRepository : JpaRepository<Notification, Long> {
	fun findByRecipientIdOrderByCreatedAtDesc(recipientId: Long, pageable: Pageable): Page<Notification>
	fun findByRecipientIdAndIsReadOrderByCreatedAtDesc(recipientId: Long, isRead: Boolean, pageable: Pageable): Page<Notification>
	fun countByRecipientIdAndIsReadFalse(recipientId: Long): Long

	// Marking read is a bulk UPDATE rather than a load-and-mutate loop: "read all" routinely covers
	// hundreds of rows, and none of the loaded state would be used for anything else.
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
	fun markAllRead(@Param("recipientId") recipientId: Long): Int

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false AND n.id IN :ids")
	fun markRead(@Param("recipientId") recipientId: Long, @Param("ids") ids: Collection<Long>): Int
}
