package team.cklob.mudda.domain.member.application.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.presentation.request.UpdateMyMemberRequest
import team.cklob.mudda.domain.member.presentation.response.MyMemberResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class UpdateMyMemberService(
	private val memberRepository: MemberRepository,
) {
	@Transactional
	fun execute(memberId: Long, request: UpdateMyMemberRequest): MyMemberResponse {
		if (request.isEmpty()) throw BusinessException(ErrorCode.INVALID_INPUT)

		val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		if (member.withdrawnAt != null) throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
		// Without this gate, an OAuth-logged-in member who never called /auth/signup could set only a
		// nickname here and skip the name/gender/birthYear requirements SignupAuthService enforces.
		if (member.nickname == null) throw BusinessException(ErrorCode.SIGNUP_REQUIRED)

		request.name?.let {
			if (it.isBlank()) throw BusinessException(ErrorCode.INVALID_INPUT)
			member.name = it.trim()
		}
		request.nickname?.let { raw ->
			if (raw.isBlank()) throw BusinessException(ErrorCode.INVALID_INPUT)
			val nickname = raw.trim()
			if (nickname != member.nickname && memberRepository.existsByNickname(nickname)) {
				throw BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS)
			}
			member.nickname = nickname
		}
		request.gender?.let { member.gender = it }
		request.birthYear?.let { member.birthYear = it }
		request.profileImageUrl?.let { member.profileImageUrl = it.trim().ifBlank { null } }
		request.bio?.let { member.bio = it.trim().ifBlank { null } }
		request.profileVisibility?.let { member.profileVisibility = it }

		val saved = try {
			memberRepository.saveAndFlush(member)
		} catch (e: DataIntegrityViolationException) {
			// The only unique constraint reachable in this transaction today is uq_member_nickname, but only
			// translate to a 409 when a nickname change was actually requested so an unrelated future
			// constraint doesn't get misreported as a nickname conflict.
			if (request.nickname != null) throw BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS)
			throw e
		}

		return MyMemberResponse.from(saved)
	}
}
