package team.cklob.mudda.domain.auth.infrastructure

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import team.cklob.mudda.global.security.AccessTokenBlacklist
import java.time.Duration
import java.time.Instant

@Component
class RedisAccessTokenBlacklist(
    private val redisTemplate: StringRedisTemplate,
) : AccessTokenBlacklist {
    override fun blacklist(jti: String, ttl: Duration) {
        redisTemplate.opsForValue().set(blacklistKey(jti), "1", ttl)
    }

    override fun isBlacklisted(jti: String): Boolean = redisTemplate.hasKey(blacklistKey(jti)) ?: false

    override fun revokeAllIssuedBefore(memberId: Long, ttl: Duration) {
        redisTemplate.opsForValue().set(revokedKey(memberId), Instant.now().toString(), ttl)
    }

    override fun isRevoked(memberId: Long, issuedAt: Instant): Boolean {
        val revokedAt = redisTemplate.opsForValue().get(revokedKey(memberId))?.let(Instant::parse) ?: return false
        return issuedAt.isBefore(revokedAt)
    }

    private fun blacklistKey(jti: String) = "auth:blacklist:$jti"

    private fun revokedKey(memberId: Long) = "auth:revoked-before:$memberId"
}
