package team.cklob.mudda.domain.auth.application.impl

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.security.JwtTokenProvider

class ReissueAuthServiceTest {
    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val refreshTokenStore = mockk<RefreshTokenStore>(relaxed = true)
    private val service = ReissueAuthService(jwtTokenProvider, refreshTokenStore)

    @Test fun `reissues tokens when the refresh token matches the stored one`() {
        every { jwtTokenProvider.validate("refresh-token") } returns true
        every { jwtTokenProvider.getMemberId("refresh-token") } returns 1L
        every { refreshTokenStore.find(1L) } returns "refresh-token"
        every { jwtTokenProvider.createAccessToken(1L) } returns "new-access-token"
        every { jwtTokenProvider.createRefreshToken(1L) } returns "new-refresh-token"

        val response = service.execute("refresh-token")

        assertEquals("new-access-token", response.accessToken)
        assertEquals("new-refresh-token", response.refreshToken)
    }

    @Test fun `rejects an invalid refresh token`() {
        every { jwtTokenProvider.validate("bad-token") } returns false

        assertThrows(AuthException::class.java) { service.execute("bad-token") }
    }

    @Test fun `rejects a refresh token that does not match the one stored in redis`() {
        every { jwtTokenProvider.validate("refresh-token") } returns true
        every { jwtTokenProvider.getMemberId("refresh-token") } returns 1L
        every { refreshTokenStore.find(1L) } returns "a-different-token"

        assertThrows(AuthException::class.java) { service.execute("refresh-token") }
    }
}
