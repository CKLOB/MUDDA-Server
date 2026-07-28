package team.cklob.mudda.domain.auth.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import team.cklob.mudda.domain.auth.application.OAuthStrategy
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.domain.auth.domain.type.OAuthUserInfo
import team.cklob.mudda.domain.auth.presentation.request.LoginAuthRequest
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.security.JwtTokenProvider
import java.util.Optional

class LoginAuthServiceTest {
    private val strategy = mockk<OAuthStrategy>()
    private val memberRepository = mockk<MemberRepository>()
    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val refreshTokenStore = mockk<RefreshTokenStore>(relaxed = true)
    private val service = LoginAuthService(listOf(strategy), memberRepository, jwtTokenProvider, refreshTokenStore)

    private val request = LoginAuthRequest("auth-code", "https://app.mudda.com/oauth/callback")
    private val userInfo = OAuthUserInfo(OAuthProvider.GOOGLE, "google-sub-1", "user@example.com")

    @Test fun `issues tokens for an existing fully signed up member`() {
        every { strategy.supports(OAuthProvider.GOOGLE) } returns true
        every { strategy.authenticate("auth-code", request.providerUri) } returns userInfo
        every { memberRepository.existsByOauthProviderAndProviderIdAndWithdrawnAtIsNotNull(OAuthProvider.GOOGLE, "google-sub-1") } returns false
        val member = Member(email = "user@example.com", oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-1", profileVisibility = "PUBLIC")
        member.nickname = "existing-nickname"
        ReflectionTestUtils.setField(member, "id", 1L)
        every { memberRepository.findByOauthProviderAndProviderIdAndWithdrawnAtIsNull(OAuthProvider.GOOGLE, "google-sub-1") } returns Optional.of(member)
        every { jwtTokenProvider.createAccessToken(1L) } returns "access-token"
        every { jwtTokenProvider.createRefreshToken(1L) } returns "refresh-token"

        val response = service.execute(OAuthProvider.GOOGLE, request)

        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
        assertFalse(response.isNewMember)
        verify { refreshTokenStore.save(1L, "refresh-token") }
    }

    @Test fun `creates an incomplete member and reports isNewMember for a brand new oauth identity`() {
        every { strategy.supports(OAuthProvider.GOOGLE) } returns true
        every { strategy.authenticate("auth-code", request.providerUri) } returns userInfo
        every { memberRepository.existsByOauthProviderAndProviderIdAndWithdrawnAtIsNotNull(OAuthProvider.GOOGLE, "google-sub-1") } returns false
        every { memberRepository.findByOauthProviderAndProviderIdAndWithdrawnAtIsNull(OAuthProvider.GOOGLE, "google-sub-1") } returns Optional.empty()
        every { memberRepository.save(any()) } answers {
            val member = firstArg<Member>()
            ReflectionTestUtils.setField(member, "id", 2L)
            member
        }
        every { jwtTokenProvider.createAccessToken(2L) } returns "access-token"
        every { jwtTokenProvider.createRefreshToken(2L) } returns "refresh-token"

        val response = service.execute(OAuthProvider.GOOGLE, request)

        assertTrue(response.isNewMember)
        assertEquals("access-token", response.accessToken)
    }

    @Test fun `rejects a withdrawn member's oauth identity`() {
        every { strategy.supports(OAuthProvider.GOOGLE) } returns true
        every { strategy.authenticate("auth-code", request.providerUri) } returns userInfo
        every { memberRepository.existsByOauthProviderAndProviderIdAndWithdrawnAtIsNotNull(OAuthProvider.GOOGLE, "google-sub-1") } returns true

        assertThrows(AuthException::class.java) { service.execute(OAuthProvider.GOOGLE, request) }
    }

    @Test fun `rejects an unsupported provider`() {
        every { strategy.supports(OAuthProvider.GOOGLE) } returns false

        assertThrows(AuthException::class.java) { service.execute(OAuthProvider.GOOGLE, request) }
    }
}
