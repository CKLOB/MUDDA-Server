package team.cklob.mudda.domain.auth.application.impl

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.auth.presentation.request.SignupAuthRequest
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.util.Optional

class SignupAuthServiceTest {
    private val memberRepository = mockk<MemberRepository>()
    private val service = SignupAuthService(memberRepository)

    private val request = SignupAuthRequest(name = "name", nickname = "nickname", gender = Gender.MALE, birthYear = 2000)

    private fun incompleteMember(): Member =
        Member(email = "user@example.com", oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-1", profileVisibility = "PUBLIC", id = 1L)

    @Test fun `completes signup for an incomplete member`() {
        val member = incompleteMember()
        every { memberRepository.findById(1L) } returns Optional.of(member)
        every { memberRepository.existsByNickname("nickname") } returns false

        service.execute(1L, request)

        assertEquals("name", member.name)
        assertEquals("nickname", member.nickname)
        assertEquals(Gender.MALE, member.gender)
        assertEquals(2000, member.birthYear)
    }

    @Test fun `rejects signup for a member that already completed it`() {
        val member = incompleteMember()
        member.nickname = "already-set"
        every { memberRepository.findById(1L) } returns Optional.of(member)

        val exception = assertThrows(BusinessException::class.java) { service.execute(1L, request) }
        assertEquals(ErrorCode.ALREADY_SIGNED_UP, exception.errorCode)
    }

    @Test fun `rejects a duplicate nickname`() {
        val member = incompleteMember()
        every { memberRepository.findById(1L) } returns Optional.of(member)
        every { memberRepository.existsByNickname("nickname") } returns true

        val exception = assertThrows(BusinessException::class.java) { service.execute(1L, request) }
        assertEquals(ErrorCode.NICKNAME_ALREADY_EXISTS, exception.errorCode)
    }
}
