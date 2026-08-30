package team.cklob.mudda.domain.friend.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestAction
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.presentation.request.RespondFriendRequestRequest
import team.cklob.mudda.domain.notification.application.impl.NotificationPublisher
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime

@Service
class RespondFriendRequestService(
	private val friendRepository: FriendRepository,
	private val blockRepository: BlockRepository,
	private val notificationPublisher: NotificationPublisher,
) {
	@Transactional
	fun execute(memberId: Long, requestId: Long, request: RespondFriendRequestRequest) {
		val friend = friendRepository.findById(requestId).orElseThrow { BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND) }
		if (friend.receiver.id != memberId) throw BusinessException(ErrorCode.FRIEND_REQUEST_NOT_RECEIVER)
		if (friend.status != FriendRequestStatus.PENDING) throw BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED)

		when (request.action) {
			FriendRequestAction.ACCEPT -> {
				val requesterId = requireNotNull(friend.requester.id)
				// SendFriendRequestService only checks for a block at the moment the request is sent. A block
				// created afterwards, while the request is still PENDING, must not be bypassed by simply
				// accepting it -- re-verify here too. (Direction doesn't need to be distinguished the way
				// SendFriendRequestService does: whichever side is blocked, the member calling this endpoint is
				// the receiver, so a BLOCKED_MEMBER response never tells them something about the other party
				// they couldn't already infer from being unable to accept.)
				if (blockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(memberId, requesterId, requesterId, memberId)) {
					throw BusinessException(ErrorCode.BLOCKED_MEMBER)
				}
				friend.status = FriendRequestStatus.ACCEPTED
				friend.acceptedAt = LocalDateTime.now()
				// Only the accepting direction notifies: a rejection is deliberately silent so the sender
				// isn't told they were turned down.
				notificationPublisher.publish(
					recipient = friend.requester,
					type = NotificationType.FRIEND_ACCEPTED,
					title = "친구 요청이 수락됐어요",
					content = "${friend.receiver.nickname ?: "누군가"}님과 친구가 되었어요.",
					targetId = memberId,
					targetType = NotificationTargetType.MEMBER,
				)
			}
			FriendRequestAction.REJECT -> {
				friend.status = FriendRequestStatus.REJECTED
			}
		}
	}
}
