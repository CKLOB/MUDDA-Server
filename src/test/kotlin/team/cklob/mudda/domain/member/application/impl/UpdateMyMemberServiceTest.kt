package team.cklob.mudda.domain.member.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.member.presentation.request.UpdateMyMemberRequest
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import java.util.Optional

class UpdateMyMemberServiceTest {
	private val memberRepository = mockk<MemberRepository>()
	private val service = UpdateMyMemberService(memberRepository)

	private fun member(withdrawnAt: LocalDateTime? = null) = Member(
		name = "name", nickname = "nickname", email = "user@example.com",
		oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-1",
		gender = Gender.MALE, birthYear = 2000, profileImageUrl = "https://img.example.com/old.png", bio = "old bio",
		profileVisibility = ProfileVisibility.PUBLIC, withdrawnAt = withdrawnAt, id = 1L,
	)

	private fun emptyRequest() = UpdateMyMemberRequest()

	@Test fun `updates only the fields present in the request`() {
		val member = member()
		every { memberRepository.findById(1L) } returns Optional.of(member)
		every { memberRepository.saveAndFlush(member) } returns member
		val request = emptyRequest().copy(bio = "new bio")

		val response = service.execute(1L, request)

		assertEquals("new bio", response.bio)
		assertEquals("name", response.name)
		assertEquals("nickname", response.nickname)
		assertEquals(Gender.MALE, response.gender)
		assertEquals(2000, response.birthYear)
		assertEquals("https://img.example.com/old.png", response.profileImageUrl)
	}

	@Test fun `allows keeping the member's own current nickname`() {
		val member = member()
		every { memberRepository.findById(1L) } returns Optional.of(member)
		every { memberRepository.saveAndFlush(member) } returns member
		val request = emptyRequest().copy(nickname = "nickname")

		val response = service.execute(1L, request)

		assertEquals("nickname", response.nickname)
	}

	@Test fun `rejects a nickname already used by another member`() {
		val member = member()
		every { memberRepository.findById(1L) } returns Optional.of(member)
		every { memberRepository.existsByNickname("taken") } returns true
		val request = emptyRequest().copy(nickname = "taken")

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, request) }
		assertEquals(ErrorCode.NICKNAME_ALREADY_EXISTS, exception.errorCode)
	}

	@Test fun `rejects a request with every field empty`() {
		every { memberRepository.findById(1L) } returns Optional.of(member())

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, emptyRequest()) }
		assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
	}

	@Test fun `rejects a blank name`() {
		every { memberRepository.findById(1L) } returns Optional.of(member())
		val request = emptyRequest().copy(name = "   ")

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, request) }
		assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
	}

	@Test fun `rejects a blank nickname`() {
		every { memberRepository.findById(1L) } returns Optional.of(member())
		val request = emptyRequest().copy(nickname = "   ")

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, request) }
		assertEquals(ErrorCode.INVALID_INPUT, exception.errorCode)
	}

	@Test fun `normalizes an empty bio and profileImageUrl to null`() {
		val member = member()
		every { memberRepository.findById(1L) } returns Optional.of(member)
		every { memberRepository.saveAndFlush(member) } returns member
		val request = emptyRequest().copy(bio = "", profileImageUrl = "")

		val response = service.execute(1L, request)

		assertNull(response.bio)
		assertNull(response.profileImageUrl)
	}

	@Test fun `does not touch auth-owned fields`() {
		val member = member()
		every { memberRepository.findById(1L) } returns Optional.of(member)
		every { memberRepository.saveAndFlush(member) } returns member
		val request = emptyRequest().copy(name = "new name")

		service.execute(1L, request)

		assertEquals("user@example.com", member.email)
		assertEquals(OAuthProvider.GOOGLE, member.oauthProvider)
		assertEquals("google-sub-1", member.providerId)
		assertNull(member.withdrawnAt)
	}

	@Test fun `rejects updates for a withdrawn member`() {
		every { memberRepository.findById(1L) } returns Optional.of(member(withdrawnAt = LocalDateTime.now()))
		val request = emptyRequest().copy(bio = "new bio")

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, request) }
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.errorCode)
	}

	@Test fun `converts a database-level nickname race into a conflict`() {
		val member = member()
		every { memberRepository.findById(1L) } returns Optional.of(member)
		every { memberRepository.existsByNickname("taken") } returns false
		every { memberRepository.saveAndFlush(member) } throws DataIntegrityViolationException("duplicate key")
		val request = emptyRequest().copy(nickname = "taken")

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, request) }
		assertEquals(ErrorCode.NICKNAME_ALREADY_EXISTS, exception.errorCode)
		verify { memberRepository.saveAndFlush(member) }
	}
}
