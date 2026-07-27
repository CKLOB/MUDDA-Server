package team.cklob.mudda.domain.auth.infrastructure

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

class GoogleOAuthStrategyTest {
    private val properties = OAuthProperties(
        kakao = OAuthProperties.Kakao(clientId = "kakao-client-id"),
        google = OAuthProperties.Google(clientId = "google-client-id", clientSecret = "google-secret"),
        apple = OAuthProperties.Apple(clientId = "apple-client-id", teamId = "team-id", keyId = "key-id", privateKey = "unused"),
    )

    private fun buildStrategy(): Pair<GoogleOAuthStrategy, MockRestServiceServer> {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        return GoogleOAuthStrategy(builder, properties) to server
    }

    @Test fun `exchanges code for token and fetches google user info`() {
        val (strategy, server) = buildStrategy()
        server.expect(ExpectedCount.once(), requestTo(properties.google.tokenUri))
            .andRespond(withSuccess("""{"access_token":"google-access-token"}""", MediaType.APPLICATION_JSON))
        server.expect(ExpectedCount.once(), requestTo(properties.google.userInfoUri))
            .andRespond(withSuccess("""{"sub":"google-sub-1","email":"user@gmail.com"}""", MediaType.APPLICATION_JSON))

        val userInfo = strategy.authenticate("auth-code", "https://app.mudda.com/oauth/callback")

        assertEquals(OAuthProvider.GOOGLE, userInfo.provider)
        assertEquals("google-sub-1", userInfo.providerId)
        assertEquals("user@gmail.com", userInfo.email)
        server.verify()
    }

    @Test fun `throws when google rejects the authorization code`() {
        val (strategy, server) = buildStrategy()
        server.expect(ExpectedCount.once(), requestTo(properties.google.tokenUri))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST))

        assertThrows(AuthException::class.java) { strategy.authenticate("bad-code", "https://app.mudda.com/oauth/callback") }
    }
}
