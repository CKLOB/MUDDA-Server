package team.cklob.mudda.domain.auth.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.auth.application.RefreshTokenStore
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.LocalDateTime

@Service
class WithdrawAuthService(
    private val memberRepository: MemberRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val accessTokenBlacklist: AccessTokenBlacklist,
    private val refreshTokenStore: RefreshTokenStore,
) {
    @Transactional
    fun execute(memberId: Long, accessToken: String) {
        val member = memberRepository.findById(memberId).orElseThrow { AuthException(ErrorCode.UNAUTHORIZED) }

        member.name = null
        member.nickname = null
        member.email = "withdrawn-$memberId@mudda.local"
        member.profileImageUrl = null
        member.bio = null
        member.withdrawnAt = LocalDateTime.now()

        accessTokenBlacklist.blacklist(jwtTokenProvider.getJti(accessToken), jwtTokenProvider.getRemainingValidity(accessToken))
        refreshTokenStore.delete(memberId)
    }
}
