package team.cklob.mudda.domain.auth.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.auth.presentation.request.SignupAuthRequest
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class SignupAuthService(
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun execute(memberId: Long, request: SignupAuthRequest) {
        val member = memberRepository.findById(memberId).orElseThrow { AuthException(ErrorCode.UNAUTHORIZED) }
        if (member.nickname != null) throw BusinessException(ErrorCode.ALREADY_SIGNED_UP)
        if (memberRepository.existsByNickname(request.nickname)) throw BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS)

        member.name = request.name
        member.nickname = request.nickname
        member.gender = request.gender
        member.birthYear = request.birthYear
    }
}
