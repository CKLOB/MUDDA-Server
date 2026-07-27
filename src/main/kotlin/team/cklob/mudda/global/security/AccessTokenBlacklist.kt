package team.cklob.mudda.global.security

import java.time.Duration

interface AccessTokenBlacklist {
    fun blacklist(jti: String, ttl: Duration)
    fun isBlacklisted(jti: String): Boolean
}
