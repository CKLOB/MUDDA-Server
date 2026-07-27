package team.cklob.mudda.domain.auth.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode

class AppleOAuthStrategyTest {
    private val strategy = AppleOAuthStrategy()

    @Test fun `supports apple but is not yet implemented`() {
        assertEquals(true, strategy.supports(OAuthProvider.APPLE))

        val exception = assertThrows(AuthException::class.java) {
            strategy.authenticate("code", "https://app.mudda.com/oauth/callback")
        }
        assertEquals(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED, exception.errorCode)
    }
}
