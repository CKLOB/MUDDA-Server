package team.cklob.mudda.domain.auth.infrastructure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("oauth")
data class OAuthProperties(
    val kakao: Kakao,
    val google: Google,
) {
    data class Kakao(
        val clientId: String,
        val clientSecret: String = "",
        val tokenUri: String = "https://kauth.kakao.com/oauth/token",
        val userInfoUri: String = "https://kapi.kakao.com/v2/user/me",
    )

    data class Google(
        val clientId: String,
        val clientSecret: String = "",
        val tokenUri: String = "https://oauth2.googleapis.com/token",
        val userInfoUri: String = "https://openidconnect.googleapis.com/v1/userinfo",
    )
}
