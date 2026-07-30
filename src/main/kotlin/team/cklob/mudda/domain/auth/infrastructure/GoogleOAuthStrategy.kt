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
class GoogleOAuthStrategy(
    restClientBuilder: RestClient.Builder,
    private val properties: OAuthProperties,
) : OAuthStrategy {
    private val restClient = restClientBuilder.build()
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(provider: OAuthProvider) = provider == OAuthProvider.GOOGLE

    override fun authenticate(code: String, redirectUri: String): OAuthUserInfo {
        val google = properties.google
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", google.clientId)
            add("client_secret", google.clientSecret)
            add("redirect_uri", redirectUri)
            add("code", code)
        }

        val tokenResponse = runCatching {
            restClient.post()
                .uri(google.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse::class.java)
        }.onFailure { logger.warn("Google token exchange failed", it) }.getOrNull() ?: throw AuthException(ErrorCode.OAUTH_INVALID_CODE)

        val userResponse = runCatching {
            restClient.get()
                .uri(google.userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenResponse.accessToken}")
                .retrieve()
                .body(GoogleUserInfoResponse::class.java)
        }.onFailure { logger.warn("Google user info request failed", it) }.getOrNull() ?: throw AuthException(ErrorCode.OAUTH_INVALID_CODE)

        val email = userResponse.email ?: "google-${userResponse.sub}@mudda.local"
        return OAuthUserInfo(OAuthProvider.GOOGLE, userResponse.sub, email)
    }
}

private data class GoogleTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
)

private data class GoogleUserInfoResponse(
    val sub: String,
    val email: String?,
)
