package team.cklob.mudda.domain.auth.infrastructure

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.global.security.JwtProperties
import java.time.Duration

@Component
class RedisRefreshTokenStore(
    private val redisTemplate: StringRedisTemplate,
    private val jwtProperties: JwtProperties,
) : RefreshTokenStore {
    override fun save(memberId: Long, refreshToken: String) {
        redisTemplate.opsForValue().set(key(memberId), refreshToken, Duration.ofMillis(jwtProperties.refreshTokenExpiration))
    }

    override fun find(memberId: Long): String? = redisTemplate.opsForValue().get(key(memberId))

    override fun delete(memberId: Long) {
        redisTemplate.delete(key(memberId))
    }

    private fun key(memberId: Long) = "auth:refresh-token:$memberId"
}
