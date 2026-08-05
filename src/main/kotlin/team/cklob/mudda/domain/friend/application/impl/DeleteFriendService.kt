package team.cklob.mudda.domain.friend.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class DeleteFriendService(
	private val friendRepository: FriendRepository,
) {
	@Transactional
	fun execute(memberId: Long, targetMemberId: Long) {
		val relations = friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(memberId, targetMemberId, targetMemberId, memberId)
		val friend = relations.firstOrNull { it.status == FriendRequestStatus.ACCEPTED } ?: throw BusinessException(ErrorCode.FRIEND_NOT_FOUND)
		friendRepository.delete(friend)
	}
}
