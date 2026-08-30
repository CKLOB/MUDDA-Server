package team.cklob.mudda.domain.notification.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.notification.domain.entity.DeviceToken
import team.cklob.mudda.domain.notification.domain.entity.Notification
import team.cklob.mudda.domain.notification.domain.repository.DeviceTokenRepository
import team.cklob.mudda.domain.notification.domain.repository.NotificationRepository
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.domain.notification.presentation.request.ReadNotificationRequest
import team.cklob.mudda.domain.notification.presentation.request.RegisterDeviceTokenRequest
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.util.Optional
import kotlin.test.assertEquals

class NotificationServicesTest {
	private val notificationRepository = mockk<NotificationRepository>()
	private val deviceTokenRepository = mockk<DeviceTokenRepository>()
	private val memberRepository = mockk<MemberRepository>()

	private fun member(id: Long) = Member(
		name = "name", nickname = "nick$id", email = "a$id@example.com", oauthProvider = OAuthProvider.GOOGLE,
		providerId = "provider$id", profileVisibility = ProfileVisibility.PUBLIC, id = id,
	)

	private fun notification(id: Long, recipient: Member) = Notification(
		recipient = recipient, notificationType = NotificationType.CAPSULE_OPENED, title = "t", content = "c",
		targetId = 9, targetType = NotificationTargetType.CAPSULE, id = id,
	)

	// -------- GetNotificationListService --------

	@Test fun `unread count ignores the isRead filter so the badge stays stable`() {
		val service = GetNotificationListService(notificationRepository)
		val page = PageImpl(listOf(notification(1, member(1))), PageRequest.of(0, 20), 1)
		every { notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(1, true, any()) } returns page
		every { notificationRepository.countByRecipientIdAndIsReadFalse(1) } returns 7

		val response = service.execute(1, isRead = true, pageable = PageRequest.of(0, 20))

		assertEquals(7, response.unreadCount)
		assertEquals(1, response.totalCount)
		assertEquals(1, response.notifications.size)
	}

	@Test fun `a null isRead filter reads the unfiltered page`() {
		val service = GetNotificationListService(notificationRepository)
		every { notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1, any()) } returns
			PageImpl(emptyList(), PageRequest.of(0, 20), 0)
		every { notificationRepository.countByRecipientIdAndIsReadFalse(1) } returns 0

		service.execute(1, isRead = null, pageable = PageRequest.of(0, 20))

		verify(exactly = 1) { notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1, any()) }
		verify(exactly = 0) { notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(any(), any(), any()) }
	}

	// -------- ReadNotificationService --------

	@Test fun `readAll marks every unread notification and ignores supplied ids`() {
		val service = ReadNotificationService(notificationRepository)
		every { notificationRepository.markAllRead(1) } returns 4

		val response = service.execute(1, ReadNotificationRequest(readAll = true, notificationIds = listOf(9)))

		assertEquals(4, response.readCount)
		verify(exactly = 0) { notificationRepository.markRead(any(), any()) }
	}

	@Test fun `marking read is scoped to the caller so other members' ids match nothing`() {
		val service = ReadNotificationService(notificationRepository)
		every { notificationRepository.markRead(1, listOf(50L)) } returns 0

		assertEquals(0, service.execute(1, ReadNotificationRequest(notificationIds = listOf(50))).readCount)
		verify(exactly = 1) { notificationRepository.markRead(1, listOf(50L)) }
	}

	@Test fun `an empty id list without readAll is rejected instead of silently doing nothing`() {
		val service = ReadNotificationService(notificationRepository)

		val error = assertThrows<BusinessException> { service.execute(1, ReadNotificationRequest()) }

		assertEquals(ErrorCode.INVALID_INPUT, error.errorCode)
	}

	// -------- DeleteNotificationService --------

	@Test fun `deleting someone else's notification reports not found rather than forbidden`() {
		val service = DeleteNotificationService(notificationRepository)
		every { notificationRepository.findById(1) } returns Optional.of(notification(1, member(2)))

		val error = assertThrows<BusinessException> { service.execute(memberId = 1, notificationId = 1) }

		assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, error.errorCode)
		verify(exactly = 0) { notificationRepository.delete(any()) }
	}

	// -------- Device tokens --------

	@Test fun `re-registering a token from another account re-points the row instead of duplicating it`() {
		val service = RegisterDeviceTokenService(deviceTokenRepository, memberRepository)
		val previousOwner = member(2)
		val newOwner = member(1)
		val existing = DeviceToken(previousOwner, "token-a", id = 5)
		every { deviceTokenRepository.findByToken("token-a") } returns Optional.of(existing)
		every { memberRepository.findById(1) } returns Optional.of(newOwner)

		service.execute(1, RegisterDeviceTokenRequest("token-a"))

		assertEquals(1, existing.member.id)
		verify(exactly = 0) { deviceTokenRepository.save(any()) }
	}

	@Test fun `a member cannot delete a device token owned by someone else`() {
		val service = DeleteDeviceTokenService(deviceTokenRepository)
		every { deviceTokenRepository.findByToken("token-a") } returns Optional.of(DeviceToken(member(2), "token-a", id = 5))

		service.execute(memberId = 1, token = "token-a")

		verify(exactly = 0) { deviceTokenRepository.delete(any()) }
	}
}
