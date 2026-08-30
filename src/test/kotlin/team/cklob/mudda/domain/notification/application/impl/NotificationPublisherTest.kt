package team.cklob.mudda.domain.notification.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.notification.domain.entity.Notification
import team.cklob.mudda.domain.notification.domain.repository.NotificationRepository
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationPublisherTest {
	private val notificationRepository = mockk<NotificationRepository>()
	private val pushSender = mockk<DevicePushSender>(relaxed = true)
	private val publisher = NotificationPublisher(notificationRepository, pushSender)

	private val recipient = Member(
		name = "name", nickname = "nick", email = "a@example.com", oauthProvider = OAuthProvider.GOOGLE,
		providerId = "provider", profileVisibility = ProfileVisibility.PUBLIC, id = 1,
	)

	// tbl_notification.content is VARCHAR(255). Overflowing it throws on flush and rolls back whatever
	// transaction raised the notification -- a capsule creation must not fail because its banner was long.
	@Test fun `content longer than the column width is truncated instead of overflowing`() {
		val saved = slot<Notification>()
		every { notificationRepository.save(capture(saved)) } answers { saved.captured }

		publisher.publish(recipient, NotificationType.CAPSULE_RECEIVED, "title", "가".repeat(400))

		assertEquals(255, saved.captured.content.length)
		assertTrue(saved.captured.content.endsWith("…"))
	}

	@Test fun `content within the column width is stored verbatim`() {
		val saved = slot<Notification>()
		every { notificationRepository.save(capture(saved)) } answers { saved.captured }

		publisher.publish(recipient, NotificationType.CAPSULE_RECEIVED, "title", "짧은 본문")

		assertEquals("짧은 본문", saved.captured.content)
		verify(exactly = 1) { pushSender.send(1L, "title", "짧은 본문") }
	}
}
