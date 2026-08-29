package team.cklob.mudda.domain.notification.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.notification.application.NotificationSender
import team.cklob.mudda.domain.notification.domain.repository.DeviceTokenRepository

class DevicePushSenderTest {
	private val deviceTokenRepository = mockk<DeviceTokenRepository>(relaxed = true)
	private val sender = mockk<NotificationSender>()
	private val pushSender = DevicePushSender(deviceTokenRepository, sender)

	@Test fun `tokens FCM rejects as permanently dead are pruned`() {
		every { deviceTokenRepository.findTokensByMemberId(1) } returns listOf("live", "dead")
		every { sender.send(any(), any(), any()) } returns listOf("dead")

		pushSender.send(1, "title", "body")

		verify(exactly = 1) { deviceTokenRepository.deleteByTokenIn(listOf("dead")) }
	}

	// A transient FCM outage must not unsubscribe every device from all future notifications.
	@Test fun `a failing send deletes nothing`() {
		every { deviceTokenRepository.findTokensByMemberId(1) } returns listOf("live")
		every { sender.send(any(), any(), any()) } throws IllegalStateException("fcm is down")

		pushSender.send(1, "title", "body")

		verify(exactly = 0) { deviceTokenRepository.deleteByTokenIn(any()) }
	}

	@Test fun `a member with no registered device is not sent to at all`() {
		every { deviceTokenRepository.findTokensByMemberId(1) } returns emptyList()

		pushSender.send(1, "title", "body")

		verify(exactly = 0) { sender.send(any(), any(), any()) }
	}
}
