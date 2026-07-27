package team.cklob.mudda.domain.auth.application.impl

import org.springframework.stereotype.Service
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.domain.auth.presentation.response.TokenResponse
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.JwtTokenProvider

@Service
class ReissueService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
) {
    fun execute(refreshToken: String): TokenResponse {
        if (!jwtTokenProvider.validate(refreshToken)) throw AuthException(ErrorCode.INVALID_REFRESH_TOKEN)

        val memberId = jwtTokenProvider.getMemberId(refreshToken)
        val storedToken = refreshTokenStore.find(memberId)
        if (storedToken == null || storedToken != refreshToken) throw AuthException(ErrorCode.INVALID_REFRESH_TOKEN)

        val newAccessToken = jwtTokenProvider.createAccessToken(memberId)
        val newRefreshToken = jwtTokenProvider.createRefreshToken(memberId)
        refreshTokenStore.save(memberId, newRefreshToken)

        return TokenResponse(newAccessToken, newRefreshToken)
    }
}
