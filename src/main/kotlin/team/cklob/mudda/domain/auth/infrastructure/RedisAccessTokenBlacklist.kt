package team.cklob.mudda.domain.auth.infrastructure

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import team.cklob.mudda.global.security.AccessTokenBlacklist
import java.time.Duration

@Component
class RedisAccessTokenBlacklist(
    private val redisTemplate: RedisTemplate<String, Any>,
) : AccessTokenBlacklist {
    override fun blacklist(jti: String, ttl: Duration) {
        redisTemplate.opsForValue().set(key(jti), true, ttl)
    }

    override fun isBlacklisted(jti: String): Boolean = redisTemplate.hasKey(key(jti))

    private fun key(jti: String) = "auth:blacklist:$jti"
}
