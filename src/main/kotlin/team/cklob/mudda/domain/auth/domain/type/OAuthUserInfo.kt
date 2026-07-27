package team.cklob.mudda.domain.auth.domain.type

import team.cklob.mudda.domain.member.domain.type.OAuthProvider

data class OAuthUserInfo(
    val provider: OAuthProvider,
    val providerId: String,
    val email: String,
)
