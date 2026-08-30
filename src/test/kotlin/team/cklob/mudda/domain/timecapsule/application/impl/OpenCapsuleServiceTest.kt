package team.cklob.mudda.domain.timecapsule.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.security.crypto.password.PasswordEncoder
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.feed.infrastructure.FeedBroadcaster
import team.cklob.mudda.domain.notification.application.impl.NotificationPublisher
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.domain.timecapsule.application.CapsuleAccessPolicy
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleOpen
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleOpenRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleRecipientRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.domain.timecapsule.presentation.request.OpenCapsuleRequest
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

class OpenCapsuleServiceTest {
	private val capsuleRepository = mockk<TimeCapsuleRepository>()
	private val openRepository = mockk<CapsuleOpenRepository>()
	private val recipientRepository = mockk<CapsuleRecipientRepository>()
	private val memberRepository = mockk<MemberRepository>()
	private val mediaRepository = mockk<MediaRepository>()
	private val mediaStorage = mockk<MediaStorage>()
	private val passwordEncoder = mockk<PasswordEncoder>()
	private val accessPolicy = mockk<CapsuleAccessPolicy>()
	private val notificationPublisher = mockk<NotificationPublisher>(relaxed = true)
	private val feedBroadcaster = mockk<FeedBroadcaster>(relaxed = true)
	private val service = OpenCapsuleService(
		capsuleRepository, openRepository, recipientRepository, memberRepository,
		mediaRepository, mediaStorage, passwordEncoder, accessPolicy,
		notificationPublisher, feedBroadcaster,
	)

	private val member = Member(
		name = "name", nickname = "nick", email = "a@example.com", oauthProvider = OAuthProvider.GOOGLE,
		providerId = "provider", profileVisibility = ProfileVisibility.PUBLIC, id = 7,
	)
	private val capsule = TimeCapsule(
		member = member, name = "capsule", content = "secret", visibility = CapsuleVisibility.PRIVATE,
		lockType = CapsuleLockType.PASSWORD, passwordHash = "hash",
		location = GeometryFactory().createPoint(Coordinate(127.0, 37.5)), openRadiusMeter = 100,
		openAt = LocalDateTime.now().minusDays(1), id = 1,
	)

	@Test
	fun `reopen still verifies location but does not verify password again`() {
		val openedAt = LocalDateTime.now().minusHours(1)
		val opened = CapsuleOpen(capsule, member, openedAt, id = 1)
		every { capsuleRepository.findByIdAndIsDeletedFalseForUpdate(1) } returns Optional.of(capsule)
		every { accessPolicy.requireAccessible(capsule, 7, any()) } returns Unit
		every { capsuleRepository.isWithinOpeningRadius(1, 37.5, 127.0) } returns true
		every { openRepository.findByTimeCapsuleIdAndMemberId(1, 7) } returns Optional.of(opened)
		every { mediaRepository.findAllByTimeCapsuleId(1) } returns emptyList()

		val response = service.execute(7, 1, OpenCapsuleRequest(37.5, 127.0))

		assertEquals("secret", response.content)
		assertEquals(openedAt, response.openedAt)
		verify(exactly = 1) { capsuleRepository.isWithinOpeningRadius(1, 37.5, 127.0) }
		verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
	}

	@Test
	fun `rejects reopen outside the capsule radius`() {
		every { capsuleRepository.findByIdAndIsDeletedFalseForUpdate(1) } returns Optional.of(capsule)
		every { accessPolicy.requireAccessible(capsule, 7, any()) } returns Unit
		every { capsuleRepository.isWithinOpeningRadius(1, 0.0, 0.0) } returns false

		val exception = assertThrows<BusinessException> { service.execute(7, 1, OpenCapsuleRequest(0.0, 0.0)) }

		assertEquals(ErrorCode.CAPSULE_OUT_OF_RANGE, exception.errorCode)
		verify(exactly = 0) { openRepository.findByTimeCapsuleIdAndMemberId(any(), any()) }
	}

	// -------- notification / feed side effects --------

	@Test fun `reopening does not notify the owner or repost to the feed again`() {
		val opened = CapsuleOpen(capsule, member, LocalDateTime.now().minusHours(1), id = 1)
		every { capsuleRepository.findByIdAndIsDeletedFalseForUpdate(1) } returns Optional.of(capsule)
		every { accessPolicy.requireAccessible(capsule, 7, any()) } returns Unit
		every { capsuleRepository.isWithinOpeningRadius(1, 37.5, 127.0) } returns true
		every { openRepository.findByTimeCapsuleIdAndMemberId(1, 7) } returns Optional.of(opened)
		every { mediaRepository.findAllByTimeCapsuleId(1) } returns emptyList()

		service.execute(7, 1, OpenCapsuleRequest(37.5, 127.0))

		verify(exactly = 0) { notificationPublisher.publish(any(), any(), any(), any(), any(), any()) }
		verify(exactly = 0) { feedBroadcaster.broadcast(any()) }
	}

	@Test fun `a first open notifies the owner and does not post a private capsule to the feed`() {
		val opener = Member(
			name = "other", nickname = "other", email = "b@example.com", oauthProvider = OAuthProvider.GOOGLE,
			providerId = "provider-2", profileVisibility = ProfileVisibility.PUBLIC, id = 8,
		)
		every { capsuleRepository.findByIdAndIsDeletedFalseForUpdate(1) } returns Optional.of(capsule)
		every { accessPolicy.requireAccessible(capsule, 8, any()) } returns Unit
		every { capsuleRepository.isWithinOpeningRadius(1, 37.5, 127.0) } returns true
		every { openRepository.findByTimeCapsuleIdAndMemberId(1, 8) } returns Optional.empty()
		every { passwordEncoder.matches("pw", "hash") } returns true
		every { memberRepository.findById(8) } returns Optional.of(opener)
		every { openRepository.save(any()) } answers { CapsuleOpen(capsule, opener, LocalDateTime.now(), id = 2) }
		every { recipientRepository.findByTimeCapsuleIdAndMemberId(1, 8) } returns Optional.empty()
		every { mediaRepository.findAllByTimeCapsuleId(1) } returns emptyList()

		service.execute(8, 1, OpenCapsuleRequest(37.5, 127.0, password = "pw"))

		verify(exactly = 1) {
			notificationPublisher.publish(member, NotificationType.CAPSULE_OPENED, any(), any(), 1L, NotificationTargetType.CAPSULE)
		}
		// The capsule under test is PRIVATE: opening it is not public information.
		verify(exactly = 0) { feedBroadcaster.broadcast(any()) }
	}

	@Test fun `opening your own capsule notifies nobody`() {
		every { capsuleRepository.findByIdAndIsDeletedFalseForUpdate(1) } returns Optional.of(capsule)
		every { accessPolicy.requireAccessible(capsule, 7, any()) } returns Unit
		every { capsuleRepository.isWithinOpeningRadius(1, 37.5, 127.0) } returns true
		every { openRepository.findByTimeCapsuleIdAndMemberId(1, 7) } returns Optional.empty()
		every { passwordEncoder.matches("pw", "hash") } returns true
		every { memberRepository.findById(7) } returns Optional.of(member)
		every { openRepository.save(any()) } answers { CapsuleOpen(capsule, member, LocalDateTime.now(), id = 3) }
		every { recipientRepository.findByTimeCapsuleIdAndMemberId(1, 7) } returns Optional.empty()
		every { mediaRepository.findAllByTimeCapsuleId(1) } returns emptyList()

		service.execute(7, 1, OpenCapsuleRequest(37.5, 127.0, password = "pw"))

		verify(exactly = 0) { notificationPublisher.publish(any(), any(), any(), any(), any(), any()) }
	}
}
