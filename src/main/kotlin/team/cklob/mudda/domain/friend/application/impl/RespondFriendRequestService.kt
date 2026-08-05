package team.cklob.mudda.domain.friend.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestAction
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.presentation.request.RespondFriendRequestRequest
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime

@Service
class RespondFriendRequestService(
	private val friendRepository: FriendRepository,
) {
	@Transactional
	fun execute(memberId: Long, requestId: Long, request: RespondFriendRequestRequest) {
		val friend = friendRepository.findById(requestId).orElseThrow { BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND) }
		if (friend.receiver.id != memberId) throw BusinessException(ErrorCode.FRIEND_REQUEST_NOT_RECEIVER)
		if (friend.status != FriendRequestStatus.PENDING) throw BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED)

		when (request.action) {
			FriendRequestAction.ACCEPT -> {
				friend.status = FriendRequestStatus.ACCEPTED
				friend.acceptedAt = LocalDateTime.now()
			}
			FriendRequestAction.REJECT -> {
				friend.status = FriendRequestStatus.REJECTED
			}
		}
	}
}
