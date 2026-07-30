package team.cklob.mudda.domain.auth.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.auth.application.OAuthStrategy
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.domain.auth.domain.type.OAuthUserInfo
import team.cklob.mudda.domain.auth.presentation.request.LoginAuthRequest
import team.cklob.mudda.domain.auth.presentation.response.LoginAuthResponse
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.LocalDateTime

@Service
class LoginAuthService(
    private val strategies: List<OAuthStrategy>,
    private val memberRepository: MemberRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
) {
    @Transactional
    fun execute(provider: OAuthProvider, request: LoginAuthRequest): LoginAuthResponse {
        val strategy = strategies.find { it.supports(provider) } ?: throw AuthException(ErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED)
        val userInfo = strategy.authenticate(request.code, request.redirectUri)

        val existing = memberRepository.findByOauthProviderAndProviderId(userInfo.provider, userInfo.providerId).orElse(null)
        val member = when {
            existing == null -> memberRepository.save(newMember(userInfo))
            existing.withdrawnAt == null -> existing
            else -> rejoin(existing, userInfo)
        }

        val memberId = requireNotNull(member.id)
        val accessToken = jwtTokenProvider.createAccessToken(memberId)
        val refreshToken = jwtTokenProvider.createRefreshToken(memberId)
        refreshTokenStore.save(memberId, refreshToken)

        return LoginAuthResponse(accessToken, refreshToken, isNewMember = member.nickname == null)
    }

    // A withdrawn member's (oauth_provider, provider_id) slot stays reserved during the grace period so the
    // withdrawal can't be used to dodge a temporary ban. After the grace period, the old row is tombstoned
    // (its provider_id is overwritten) so the real provider id is freed up for a brand new signup.
    private fun rejoin(withdrawnMember: Member, userInfo: OAuthUserInfo): Member {
        val withdrawnAt = requireNotNull(withdrawnMember.withdrawnAt)
        if (withdrawnAt.isAfter(LocalDateTime.now().minusDays(WITHDRAWAL_GRACE_PERIOD_DAYS))) {
            throw AuthException(ErrorCode.WITHDRAWN_MEMBER)
        }

        withdrawnMember.providerId = "withdrawn-${withdrawnMember.id}-${withdrawnMember.providerId}"
        memberRepository.save(withdrawnMember)
        return memberRepository.save(newMember(userInfo))
    }

    private fun newMember(userInfo: OAuthUserInfo) = Member(
        email = userInfo.email,
        oauthProvider = userInfo.provider,
        providerId = userInfo.providerId,
        profileVisibility = DEFAULT_PROFILE_VISIBILITY,
    )

    private companion object {
        const val DEFAULT_PROFILE_VISIBILITY = "PUBLIC"
        const val WITHDRAWAL_GRACE_PERIOD_DAYS = 30L
    }
}
