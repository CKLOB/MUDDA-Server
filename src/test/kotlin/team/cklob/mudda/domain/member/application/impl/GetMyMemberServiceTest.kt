package team.cklob.mudda.domain.member.application.impl

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import java.util.Optional

class GetMyMemberServiceTest {
	private val memberRepository = mockk<MemberRepository>()
	private val service = GetMyMemberService(memberRepository)

	private fun member(withdrawnAt: LocalDateTime? = null) = Member(
		name = "name", nickname = "nickname", email = "user@example.com",
		oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-1",
		gender = Gender.MALE, birthYear = 2000, bio = "hello",
		profileVisibility = ProfileVisibility.PUBLIC, withdrawnAt = withdrawnAt, id = 1L,
	)

	@Test fun `returns the current member's data`() {
		val member = member()
		every { memberRepository.findById(1L) } returns Optional.of(member)

		val response = service.execute(1L)

		assertEquals(1L, response.memberId)
		assertEquals("name", response.name)
		assertEquals("nickname", response.nickname)
		assertEquals(Gender.MALE, response.gender)
		assertEquals(2000, response.birthYear)
		assertEquals("hello", response.bio)
		assertEquals(ProfileVisibility.PUBLIC, response.profileVisibility)
	}

	@Test fun `rejects a member id that does not exist`() {
		every { memberRepository.findById(1L) } returns Optional.empty()

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L) }
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.errorCode)
	}

	@Test fun `rejects a withdrawn member`() {
		every { memberRepository.findById(1L) } returns Optional.of(member(withdrawnAt = LocalDateTime.now()))

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L) }
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.errorCode)
	}
}
