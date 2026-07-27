package team.cklob.mudda.domain.auth.infrastructure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class AppleOAuthStrategyTest {
    private val objectMapper = jacksonObjectMapper()
    private val properties = OAuthProperties(
        kakao = OAuthProperties.Kakao(clientId = "kakao-client-id"),
        google = OAuthProperties.Google(clientId = "google-client-id", clientSecret = "google-secret"),
        apple = OAuthProperties.Apple(
            clientId = "com.mudda.app.service",
            teamId = "TEAMID1234",
            keyId = "KEYID1234",
            privateKey = generateTestPrivateKeyPem(),
        ),
    )

    private fun buildStrategy(): Pair<AppleOAuthStrategy, MockRestServiceServer> {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        return AppleOAuthStrategy(builder, properties, objectMapper) to server
    }

    private fun fakeIdToken(sub: String, email: String?): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"RS256"}""".toByteArray())
        val payloadJson = if (email != null) """{"sub":"$sub","email":"$email"}""" else """{"sub":"$sub"}"""
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray())
        return "$header.$payload.fake-signature"
    }

    private fun generateTestPrivateKeyPem(): String {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val encoded = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        return "-----BEGIN PRIVATE KEY-----\n$encoded\n-----END PRIVATE KEY-----"
    }

    @Test fun `exchanges code for token and decodes apple id token`() {
        val (strategy, server) = buildStrategy()
        val idToken = fakeIdToken("apple-user-1", "user@icloud.com")
        server.expect(ExpectedCount.once(), requestTo(properties.apple.tokenUri))
            .andRespond(withSuccess("""{"id_token":"$idToken"}""", MediaType.APPLICATION_JSON))

        val userInfo = strategy.authenticate("auth-code", "https://app.mudda.com/oauth/callback")

        assertEquals(OAuthProvider.APPLE, userInfo.provider)
        assertEquals("apple-user-1", userInfo.providerId)
        assertEquals("user@icloud.com", userInfo.email)
        server.verify()
    }

    @Test fun `throws when apple rejects the authorization code`() {
        val (strategy, server) = buildStrategy()
        server.expect(ExpectedCount.once(), requestTo(properties.apple.tokenUri))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST))

        assertThrows(AuthException::class.java) { strategy.authenticate("bad-code", "https://app.mudda.com/oauth/callback") }
    }

    @Test fun `throws when the apple id token has no email`() {
        val (strategy, server) = buildStrategy()
        val idToken = fakeIdToken("apple-user-1", null)
        server.expect(ExpectedCount.once(), requestTo(properties.apple.tokenUri))
            .andRespond(withSuccess("""{"id_token":"$idToken"}""", MediaType.APPLICATION_JSON))

        assertThrows(AuthException::class.java) { strategy.authenticate("auth-code", "https://app.mudda.com/oauth/callback") }
    }
}
