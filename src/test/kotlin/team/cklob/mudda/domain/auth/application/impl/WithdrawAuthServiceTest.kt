package team.cklob.mudda.domain.auth.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.Duration
import java.util.Optional

class WithdrawAuthServiceTest {
    private val memberRepository = mockk<MemberRepository>()
    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val accessTokenBlacklist = mockk<AccessTokenBlacklist>(relaxed = true)
    private val refreshTokenStore = mockk<RefreshTokenStore>(relaxed = true)
    private val service = WithdrawAuthService(memberRepository, jwtTokenProvider, accessTokenBlacklist, refreshTokenStore)

    @Test fun `soft deletes and anonymizes the member, then revokes tokens`() {
        val member = Member(
            name = "name", nickname = "nickname", email = "user@example.com",
            oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-1", profileVisibility = ProfileVisibility.PUBLIC, id = 1L,
        )
        every { memberRepository.findById(1L) } returns Optional.of(member)
        every { jwtTokenProvider.getJti("access-token") } returns "jti-1"
        every { jwtTokenProvider.getRemainingValidity("access-token") } returns Duration.ofMinutes(30)
        every { jwtTokenProvider.getAccessTokenMaxTtl() } returns Duration.ofHours(1)

        service.execute(1L, "access-token")

        assertNull(member.name)
        assertNull(member.nickname)
        assertEquals("withdrawn-1@mudda.local", member.email)
        assertNotNull(member.withdrawnAt)
        verify { accessTokenBlacklist.blacklist("jti-1", Duration.ofMinutes(30)) }
        verify { accessTokenBlacklist.revokeAllIssuedBefore(1L, Duration.ofHours(1)) }
        verify { refreshTokenStore.delete(1L) }
    }
}
