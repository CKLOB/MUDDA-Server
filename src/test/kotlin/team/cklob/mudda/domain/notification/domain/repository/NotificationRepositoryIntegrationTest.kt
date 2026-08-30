package team.cklob.mudda.domain.notification.domain.repository

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.notification.domain.entity.DeviceToken
import team.cklob.mudda.domain.notification.domain.entity.Notification
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.support.PostgresIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Runs the real V7 migration against PostgreSQL: the renamed content column, the new target columns, and
// the device token table all have to line up with the entity mappings, which a MockK test cannot check.
class NotificationRepositoryIntegrationTest(
	@Autowired private val notificationRepository: NotificationRepository,
	@Autowired private val deviceTokenRepository: DeviceTokenRepository,
	@Autowired private val memberRepository: MemberRepository,
) : PostgresIntegrationTest() {
	private fun member(suffix: String) = memberRepository.save(
		Member(
			name = "name", nickname = "nick-$suffix", email = "$suffix@example.com",
			oauthProvider = OAuthProvider.GOOGLE, providerId = "provider-$suffix",
			profileVisibility = ProfileVisibility.PUBLIC,
		),
	)

	private fun notification(recipient: Member, isRead: Boolean) = notificationRepository.save(
		Notification(
			recipient = recipient, notificationType = NotificationType.CAPSULE_OPENED,
			title = "title", content = "content", targetId = 42,
			targetType = NotificationTargetType.CAPSULE, isRead = isRead,
		),
	)

	@Test fun `target columns round-trip through the V7 schema`() {
		val recipient = member("target")
		val saved = notification(recipient, isRead = false)

		val found = notificationRepository.findById(requireNotNull(saved.id)).orElseThrow()

		assertEquals(42, found.targetId)
		assertEquals(NotificationTargetType.CAPSULE, found.targetType)
		assertEquals("content", found.content)
	}

	@Test fun `the isRead filter and the unread count are scoped to one recipient`() {
		val mine = member("mine")
		val theirs = member("theirs")
		notification(mine, isRead = false)
		notification(mine, isRead = false)
		notification(mine, isRead = true)
		notification(theirs, isRead = false)

		val unreadPage = notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(
			requireNotNull(mine.id), false, PageRequest.of(0, 20),
		)

		assertEquals(2, unreadPage.totalElements)
		assertEquals(3, notificationRepository.findByRecipientIdOrderByCreatedAtDesc(requireNotNull(mine.id), PageRequest.of(0, 20)).totalElements)
		assertEquals(2, notificationRepository.countByRecipientIdAndIsReadFalse(requireNotNull(mine.id)))
	}

	@Test fun `markAllRead only touches the caller's unread rows`() {
		val mine = member("bulk-mine")
		val theirs = member("bulk-theirs")
		notification(mine, isRead = false)
		notification(mine, isRead = true)
		notification(theirs, isRead = false)

		// Already-read rows are excluded, so the count reflects real work rather than the row total.
		assertEquals(1, notificationRepository.markAllRead(requireNotNull(mine.id)))
		assertEquals(0, notificationRepository.countByRecipientIdAndIsReadFalse(requireNotNull(mine.id)))
		assertEquals(1, notificationRepository.countByRecipientIdAndIsReadFalse(requireNotNull(theirs.id)))
	}

	@Test fun `markRead ignores ids belonging to another member`() {
		val mine = member("scoped-mine")
		val theirs = member("scoped-theirs")
		val ours = notification(mine, isRead = false)
		val yours = notification(theirs, isRead = false)

		val readCount = notificationRepository.markRead(
			requireNotNull(mine.id), listOf(requireNotNull(ours.id), requireNotNull(yours.id)),
		)

		assertEquals(1, readCount)
		assertEquals(1, notificationRepository.countByRecipientIdAndIsReadFalse(requireNotNull(theirs.id)))
	}

	@Test fun `device tokens are looked up by member and pruned in bulk`() {
		val owner = member("device")
		deviceTokenRepository.save(DeviceToken(owner, "token-live"))
		deviceTokenRepository.save(DeviceToken(owner, "token-dead"))

		assertEquals(2, deviceTokenRepository.findTokensByMemberId(requireNotNull(owner.id)).size)

		deviceTokenRepository.deleteByTokenIn(listOf("token-dead"))

		val remaining = deviceTokenRepository.findTokensByMemberId(requireNotNull(owner.id))
		assertEquals(listOf("token-live"), remaining)
		assertTrue(deviceTokenRepository.findByToken("token-dead").isEmpty)
	}
}
