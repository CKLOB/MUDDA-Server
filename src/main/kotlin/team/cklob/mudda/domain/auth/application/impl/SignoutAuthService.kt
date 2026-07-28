package team.cklob.mudda.domain.auth.application.impl

import org.springframework.stereotype.Service
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider

@Service
class SignoutAuthService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val accessTokenBlacklist: AccessTokenBlacklist,
    private val refreshTokenStore: RefreshTokenStore,
) {
    fun execute(memberId: Long, accessToken: String) {
        accessTokenBlacklist.blacklist(jwtTokenProvider.getJti(accessToken), jwtTokenProvider.getRemainingValidity(accessToken))
        refreshTokenStore.delete(memberId)
    }
}
