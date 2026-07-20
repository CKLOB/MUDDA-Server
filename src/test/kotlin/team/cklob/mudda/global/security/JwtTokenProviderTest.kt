package team.cklob.mudda.global.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {
    private val provider = JwtTokenProvider(JwtProperties("test-secret-that-is-at-least-thirty-two-bytes", 60_000, 120_000))

    @Test fun `creates and validates access and refresh tokens`() {
        val access = provider.createAccessToken(1)
        val refresh = provider.createRefreshToken(1)

        assertTrue(provider.validate(access)); assertTrue(provider.validate(refresh)); assertEquals(1, provider.getMemberId(access))
    }

    @Test fun `rejects expired token`() {
        val expired = JwtTokenProvider(JwtProperties("test-secret-that-is-at-least-thirty-two-bytes", -1, -1)).createAccessToken(1)
        assertFalse(provider.validate(expired))
    }
}
