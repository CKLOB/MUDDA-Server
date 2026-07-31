package team.cklob.mudda.domain.auth.application

import team.cklob.mudda.domain.auth.domain.type.OAuthUserInfo
import team.cklob.mudda.domain.member.domain.type.OAuthProvider

interface OAuthStrategy {
    fun supports(provider: OAuthProvider): Boolean
    fun authenticate(code: String, redirectUri: String): OAuthUserInfo
}
