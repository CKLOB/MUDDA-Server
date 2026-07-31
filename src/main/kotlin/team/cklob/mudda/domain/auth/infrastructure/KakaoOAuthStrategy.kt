package team.cklob.mudda.domain.auth.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import team.cklob.mudda.domain.auth.application.OAuthStrategy
import team.cklob.mudda.domain.auth.domain.type.OAuthUserInfo
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode

@Component
class KakaoOAuthStrategy(
    restClientBuilder: RestClient.Builder,
    private val properties: OAuthProperties,
) : OAuthStrategy {
    private val restClient = restClientBuilder.build()
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(provider: OAuthProvider) = provider == OAuthProvider.KAKAO

    override fun authenticate(code: String, redirectUri: String): OAuthUserInfo {
        val kakao = properties.kakao
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", kakao.clientId)
            if (kakao.clientSecret.isNotBlank()) add("client_secret", kakao.clientSecret)
            add("redirect_uri", redirectUri)
            add("code", code)
        }

        val tokenResponse = runCatching {
            restClient.post()
                .uri(kakao.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse::class.java)
        }.onFailure { logger.warn("Kakao token exchange failed", it) }.getOrNull() ?: throw AuthException(ErrorCode.OAUTH_INVALID_CODE)

        val userResponse = runCatching {
            restClient.get()
                .uri(kakao.userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenResponse.accessToken}")
                .retrieve()
                .body(KakaoUserResponse::class.java)
        }.onFailure { logger.warn("Kakao user info request failed", it) }.getOrNull() ?: throw AuthException(ErrorCode.OAUTH_INVALID_CODE)

        // Email consent is optional on Kakao; the real identifier is providerId, so fall back to a
        // synthetic address instead of hard-blocking login (same pattern as withdrawal's anonymized email).
        val email = userResponse.kakaoAccount?.email ?: "kakao-${userResponse.id}@mudda.local"
        return OAuthUserInfo(OAuthProvider.KAKAO, userResponse.id.toString(), email)
    }
}

private data class KakaoTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
)

private data class KakaoUserResponse(
    val id: Long,
    @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount?,
)

private data class KakaoAccount(
    val email: String?,
)
