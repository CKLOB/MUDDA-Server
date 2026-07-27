package team.cklob.mudda.domain.auth.infrastructure

import org.springframework.stereotype.Component
import team.cklob.mudda.domain.auth.application.OAuthStrategy
import team.cklob.mudda.domain.auth.domain.type.OAuthUserInfo
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode

// Apple has no confirmed API contract yet; a real implementation can drop in here without touching callers.
@Component
class AppleOAuthStrategy : OAuthStrategy {
    override fun supports(provider: OAuthProvider) = provider == OAuthProvider.APPLE

    override fun authenticate(code: String, redirectUri: String): OAuthUserInfo =
        throw AuthException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED)
}
