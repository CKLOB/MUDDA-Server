package team.cklob.mudda.domain.auth.application.impl

import org.springframework.stereotype.Service
import team.cklob.mudda.domain.auth.application.OAuthStrategy
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.domain.auth.presentation.request.OAuthLoginRequest
import team.cklob.mudda.domain.auth.presentation.response.OAuthLoginResponse
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.JwtTokenProvider

@Service
class OAuthLoginService(
    private val strategies: List<OAuthStrategy>,
    private val memberRepository: MemberRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
) {
    fun execute(provider: OAuthProvider, request: OAuthLoginRequest): OAuthLoginResponse {
        val strategy = strategies.find { it.supports(provider) } ?: throw AuthException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED)
        val userInfo = strategy.authenticate(request.code, request.providerUri)

        if (memberRepository.existsByOauthProviderAndProviderIdAndWithdrawnAtIsNotNull(userInfo.provider, userInfo.providerId)) {
            throw AuthException(ErrorCode.WITHDRAWN_MEMBER)
        }

        val member = memberRepository.findByOauthProviderAndProviderIdAndWithdrawnAtIsNull(userInfo.provider, userInfo.providerId)
            .orElseGet {
                memberRepository.save(
                    Member(
                        email = userInfo.email,
                        oauthProvider = userInfo.provider,
                        providerId = userInfo.providerId,
                        profileVisibility = DEFAULT_PROFILE_VISIBILITY,
                    ),
                )
            }

        val memberId = requireNotNull(member.id)
        val accessToken = jwtTokenProvider.createAccessToken(memberId)
        val refreshToken = jwtTokenProvider.createRefreshToken(memberId)
        refreshTokenStore.save(memberId, refreshToken)

        return OAuthLoginResponse(accessToken, refreshToken, isNewMember = member.nickname == null)
    }

    private companion object {
        const val DEFAULT_PROFILE_VISIBILITY = "PUBLIC"
    }
}
