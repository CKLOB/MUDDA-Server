package team.cklob.mudda.domain.auth.infrastructure

import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import team.cklob.mudda.global.security.JwtProperties
import java.time.Duration
import java.time.Instant

// Guards against silent (de)serialization mismatches between StringRedisTemplate and what the
// blacklist/refresh-token adapters actually write/read -- unit tests with mocked RedisTemplate
// wouldn't catch this since Redis itself is never exercised.
@Testcontainers
class RedisAuthAdaptersIntegrationTest {
    @Test fun `access token blacklist round-trips through redis`() {
        val blacklist = RedisAccessTokenBlacklist(redisTemplate)

        assertFalse(blacklist.isBlacklisted("jti-1"))
        blacklist.blacklist("jti-1", Duration.ofMinutes(1))
        assertTrue(blacklist.isBlacklisted("jti-1"))
    }

    @Test fun `member-wide revocation rejects tokens issued before the revoked-at timestamp`() {
        val blacklist = RedisAccessTokenBlacklist(redisTemplate)
        val issuedBeforeRevocation = Instant.now().minusSeconds(5)

        assertFalse(blacklist.isRevoked(42L, issuedBeforeRevocation))
        blacklist.revokeAllIssuedBefore(42L, Duration.ofMinutes(1))
        assertTrue(blacklist.isRevoked(42L, issuedBeforeRevocation))
        assertFalse(blacklist.isRevoked(42L, Instant.now().plusSeconds(5)))
    }

    @Test fun `refresh token store round-trips save, find and delete`() {
        val store = RedisRefreshTokenStore(redisTemplate, JwtProperties("test-secret-that-is-at-least-thirty-two-bytes", 60_000, 120_000))

        assertNull(store.find(1L))
        store.save(1L, "refresh-token-value")
        assertEquals("refresh-token-value", store.find(1L))
        store.delete(1L)
        assertNull(store.find(1L))
    }

    companion object {
        @Container
        @JvmStatic
        val redis = RedisContainer(DockerImageName.parse("redis:7-alpine"))

        private lateinit var connectionFactory: LettuceConnectionFactory
        lateinit var redisTemplate: StringRedisTemplate

        @BeforeAll
        @JvmStatic
        fun setUp() {
            connectionFactory = LettuceConnectionFactory(RedisStandaloneConfiguration(redis.host, redis.firstMappedPort))
            connectionFactory.afterPropertiesSet()
            redisTemplate = StringRedisTemplate(connectionFactory).apply { afterPropertiesSet() }
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            connectionFactory.destroy()
        }
    }
}
