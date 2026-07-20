package team.cklob.mudda.global.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtTokenProvider(properties: JwtProperties) {
    private val key = Keys.hmacShaKeyFor(properties.secret.toByteArray())
    private val accessExpiration = properties.accessTokenExpiration
    private val refreshExpiration = properties.refreshTokenExpiration

    fun createAccessToken(memberId: Long) = createToken(memberId, accessExpiration)
    fun createRefreshToken(memberId: Long) = createToken(memberId, refreshExpiration)
    fun getMemberId(token: String): Long = claims(token).subject.toLong()
    fun validate(token: String): Boolean = runCatching { claims(token) }.isSuccess

    private fun createToken(memberId: Long, expiration: Long): String = Jwts.builder()
        .subject(memberId.toString()).issuedAt(Date()).expiration(Date(System.currentTimeMillis() + expiration))
        .signWith(key).compact()

    private fun claims(token: String) = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}
