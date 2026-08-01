package team.cklob.mudda.domain.member.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.presentation.response.MyMemberResponse
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class GetMyMemberService(
	private val memberRepository: MemberRepository,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long): MyMemberResponse {
		// Matches the Auth domain's handling of an invalid token subject (see SignupAuthService,
		// WithdrawAuthService) so a client's "401 -> re-login" rule doesn't need a /member/me exception.
		val member = memberRepository.findById(memberId).orElseThrow { AuthException(ErrorCode.UNAUTHORIZED) }
		if (member.withdrawnAt != null) throw BusinessException(ErrorCode.WITHDRAWN_MEMBER)
		return MyMemberResponse.from(member)
	}
}
