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
	private val service = OpenCapsuleService(
		capsuleRepository, openRepository, recipientRepository, memberRepository,
		mediaRepository, mediaStorage, passwordEncoder, accessPolicy,
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
		every { capsuleRepository.findByIdAndIsDeletedFalse(1) } returns Optional.of(capsule)
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
		every { capsuleRepository.findByIdAndIsDeletedFalse(1) } returns Optional.of(capsule)
		every { accessPolicy.requireAccessible(capsule, 7, any()) } returns Unit
		every { capsuleRepository.isWithinOpeningRadius(1, 0.0, 0.0) } returns false

		val exception = assertThrows<BusinessException> { service.execute(7, 1, OpenCapsuleRequest(0.0, 0.0)) }

		assertEquals(ErrorCode.CAPSULE_OUT_OF_RANGE, exception.errorCode)
		verify(exactly = 0) { openRepository.findByTimeCapsuleIdAndMemberId(any(), any()) }
	}
}
