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

	private fun resolveFriendStatus(viewerId: Long, memberId: Long): FriendStatus {
		val friend = friendRepository
			.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(viewerId, memberId, memberId, viewerId)
			.orElse(null) ?: return FriendStatus.NONE

		return when (friend.status) {
			FriendRequestStatus.ACCEPTED -> FriendStatus.FRIEND
			FriendRequestStatus.REJECTED -> FriendStatus.NONE
			FriendRequestStatus.PENDING -> if (friend.requester.id == viewerId) FriendStatus.REQUESTED else FriendStatus.RECEIVED
		}
	}
}
