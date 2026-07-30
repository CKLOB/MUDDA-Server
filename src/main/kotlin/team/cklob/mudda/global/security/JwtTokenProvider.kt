package team.cklob.mudda.global.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtTokenProvider(properties: JwtProperties) {
    private val key = Keys.hmacShaKeyFor(properties.secret.toByteArray())
    private val accessExpiration = properties.accessTokenExpiration
    private val refreshExpiration = properties.refreshTokenExpiration

    fun createAccessToken(memberId: Long) = createToken(memberId, accessExpiration, TOKEN_TYPE_ACCESS)
    fun createRefreshToken(memberId: Long) = createToken(memberId, refreshExpiration, TOKEN_TYPE_REFRESH)
    fun getMemberId(token: String): Long = claims(token).subject.toLong()
    fun getJti(token: String): String? = claims(token).id
    fun validate(token: String): Boolean = runCatching { claims(token) }.isSuccess
    fun isRefreshToken(token: String): Boolean = claims(token)[CLAIM_TYPE] == TOKEN_TYPE_REFRESH

    // Parses the token once and returns its claims only if it is a well-formed, non-blacklisted-eligible access token;
    // used by the request-hot-path filter to avoid re-parsing/re-verifying the signature three separate times per request.
    fun parseAccessToken(token: String): Claims? =
        runCatching { claims(token) }.getOrNull()?.takeIf { it[CLAIM_TYPE] == TOKEN_TYPE_ACCESS }

    fun getRemainingValidity(token: String): Duration {
        val remainingMillis = claims(token).expiration.time - System.currentTimeMillis()
        return Duration.ofMillis(remainingMillis).coerceAtLeast(Duration.ofSeconds(1))
    }

    // Upper bound on how long an access token can remain valid; used to size member-wide revocation entries.
    fun getAccessTokenMaxTtl(): Duration = Duration.ofMillis(accessExpiration)

    private fun createToken(memberId: Long, expiration: Long, type: String): String = Jwts.builder()
        .id(UUID.randomUUID().toString())
        .claim(CLAIM_TYPE, type)
        .subject(memberId.toString()).issuedAt(Date()).expiration(Date(System.currentTimeMillis() + expiration))
        .signWith(key).compact()

    private fun claims(token: String) = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    private companion object {
        const val CLAIM_TYPE = "typ"
        const val TOKEN_TYPE_ACCESS = "access"
        const val TOKEN_TYPE_REFRESH = "refresh"
    }
}
