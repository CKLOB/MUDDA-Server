package team.cklob.mudda.domain.auth.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import team.cklob.mudda.domain.auth.application.OAuthStrategy
import team.cklob.mudda.domain.auth.domain.type.OAuthUserInfo
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.Base64
import java.util.Date

// Apple's token endpoint is the only source of truth we call here: the code-for-token exchange
// happens over a TLS + client_secret-authenticated server-to-server channel, so the returned
// id_token is trusted without an extra JWKS signature check (same reasoning as Google/Kakao).
@Component
class AppleOAuthStrategy(
    restClientBuilder: RestClient.Builder,
    private val properties: OAuthProperties,
    private val objectMapper: ObjectMapper,
) : OAuthStrategy {
    private val restClient = restClientBuilder.build()
    private val privateKey: PrivateKey by lazy { parsePrivateKey(properties.apple.privateKey) }

    override fun supports(provider: OAuthProvider) = provider == OAuthProvider.APPLE

    override fun authenticate(code: String, redirectUri: String): OAuthUserInfo {
        val apple = properties.apple
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", apple.clientId)
            add("client_secret", createClientSecret(apple))
            add("redirect_uri", redirectUri)
            add("code", code)
        }

        val tokenResponse = runCatching {
            restClient.post()
                .uri(apple.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(AppleTokenResponse::class.java)
        }.getOrNull() ?: throw AuthException(ErrorCode.OAUTH_INVALID_CODE)

        val claims = decodeIdTokenPayload(tokenResponse.idToken)
        val email = claims.email ?: throw AuthException(ErrorCode.OAUTH_EMAIL_REQUIRED)
        return OAuthUserInfo(OAuthProvider.APPLE, claims.sub, email)
    }

    private fun createClientSecret(apple: OAuthProperties.Apple): String {
        val now = Date()
        return Jwts.builder()
            .header().add("kid", apple.keyId).and()
            .issuer(apple.teamId)
            .subject(apple.clientId)
            .audience().add("https://appleid.apple.com").and()
            .issuedAt(now)
            .expiration(Date(now.time + Duration.ofMinutes(5).toMillis()))
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact()
    }

    private fun decodeIdTokenPayload(idToken: String): AppleIdTokenClaims {
        val payload = runCatching { idToken.split(".")[1] }.getOrNull() ?: throw AuthException(ErrorCode.OAUTH_INVALID_CODE)
        val decoded = String(Base64.getUrlDecoder().decode(payload))
        return objectMapper.readValue(decoded, AppleIdTokenClaims::class.java)
    }

    private fun parsePrivateKey(pem: String): PrivateKey {
        val cleaned = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "")
            .replace(Regex("\\s"), "")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(cleaned))
        return KeyFactory.getInstance("EC").generatePrivate(keySpec)
    }
}

private data class AppleTokenResponse(
    @JsonProperty("id_token") val idToken: String,
)

private data class AppleIdTokenClaims(
    val sub: String,
    val email: String?,
)
