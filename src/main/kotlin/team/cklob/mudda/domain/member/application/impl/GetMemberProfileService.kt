package team.cklob.mudda.domain.member.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.application.ProfileAccessPolicy
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.presentation.response.MemberProfileResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class GetMemberProfileService(
	private val memberRepository: MemberRepository,
	private val friendRepository: FriendRepository,
) {
	@Transactional(readOnly = true)
	fun execute(viewerId: Long, memberId: Long): MemberProfileResponse {
		val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		if (member.withdrawnAt != null || member.nickname == null) throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)

		val isSelf = viewerId == memberId
		val friendStatus = if (isSelf) FriendStatus.NONE else resolveFriendStatus(viewerId, memberId)

		if (!ProfileAccessPolicy.canView(member.profileVisibility, isSelf, friendStatus)) {
			throw BusinessException(ErrorCode.PROFILE_ACCESS_DENIED)
		}

		return MemberProfileResponse.of(member, friendStatus)
	}

	// A requester/receiver pair can have relationship rows in both directions (see FriendRepository), so an
	// ACCEPTED row always wins over a stray PENDING row for the same pair.
	private fun resolveFriendStatus(viewerId: Long, memberId: Long): FriendStatus {
		val relations = friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(viewerId, memberId, memberId, viewerId)
		if (relations.any { it.status == FriendRequestStatus.ACCEPTED }) return FriendStatus.FRIEND

		val pending = relations.firstOrNull { it.status == FriendRequestStatus.PENDING } ?: return FriendStatus.NONE
		return if (pending.requester.id == viewerId) FriendStatus.REQUESTED else FriendStatus.RECEIVED
	}
}
