package team.cklob.mudda.domain.auth.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.Duration

class SignoutServiceTest {
    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val accessTokenBlacklist = mockk<AccessTokenBlacklist>(relaxed = true)
    private val refreshTokenStore = mockk<RefreshTokenStore>(relaxed = true)
    private val service = SignoutService(jwtTokenProvider, accessTokenBlacklist, refreshTokenStore)

    @Test fun `blacklists the access token and deletes the stored refresh token`() {
        every { jwtTokenProvider.getJti("access-token") } returns "jti-1"
        every { jwtTokenProvider.getRemainingValidity("access-token") } returns Duration.ofMinutes(30)

        service.execute(1L, "access-token")

        verify { accessTokenBlacklist.blacklist("jti-1", Duration.ofMinutes(30)) }
        verify { refreshTokenStore.delete(1L) }
    }
}
