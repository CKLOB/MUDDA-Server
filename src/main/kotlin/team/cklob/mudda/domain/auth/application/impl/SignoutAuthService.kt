package team.cklob.mudda.domain.auth.application.impl

import org.springframework.stereotype.Service
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider

@Service
class SignoutAuthService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val accessTokenBlacklist: AccessTokenBlacklist,
    private val refreshTokenStore: RefreshTokenStore,
) {
    fun execute(memberId: Long, accessToken: String) {
        val jti = jwtTokenProvider.getJti(accessToken) ?: throw AuthException(ErrorCode.INVALID_TOKEN)
        accessTokenBlacklist.blacklist(jti, jwtTokenProvider.getRemainingValidity(accessToken))
        refreshTokenStore.delete(memberId)
    }
}
