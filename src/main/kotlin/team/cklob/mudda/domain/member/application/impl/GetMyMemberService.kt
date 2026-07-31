package team.cklob.mudda.domain.member.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.presentation.response.MyMemberResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class GetMyMemberService(
	private val memberRepository: MemberRepository,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long): MyMemberResponse {
		val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		if (member.withdrawnAt != null) throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
		return MyMemberResponse.from(member)
	}
}
