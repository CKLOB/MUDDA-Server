package team.cklob.mudda.global.security

import java.time.Duration
import java.time.Instant

interface AccessTokenBlacklist {
    fun blacklist(jti: String, ttl: Duration)
    fun isBlacklisted(jti: String): Boolean

    // Member-wide revocation (e.g. withdrawal), independent of any single token's jti so it
    // also invalidates access tokens already issued to other devices/sessions.
    fun revokeAllIssuedBefore(memberId: Long, ttl: Duration)
    fun isRevoked(memberId: Long, issuedAt: Instant): Boolean
}
