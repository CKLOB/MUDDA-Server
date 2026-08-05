package team.cklob.mudda.domain.friend.application.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.presentation.request.SendFriendRequestRequest
import team.cklob.mudda.domain.friend.presentation.response.SendFriendRequestResponse
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class SendFriendRequestService(
	private val friendRepository: FriendRepository,
	private val memberRepository: MemberRepository,
	private val blockRepository: BlockRepository,
) {
	@Transactional
	fun execute(memberId: Long, request: SendFriendRequestRequest): SendFriendRequestResponse {
		// @field:NotNull on SendFriendRequestRequest.receiverId already rejects a null/missing value with a
		// 400 before this service runs; requireNotNull here just documents that invariant for callers.
		val receiverId = requireNotNull(request.receiverId)
		if (memberId == receiverId) throw BusinessException(ErrorCode.CANNOT_REQUEST_SELF)

		val requester = memberRepository.findById(memberId).orElseThrow { AuthException(ErrorCode.UNAUTHORIZED) }
		if (requester.withdrawnAt != null) throw BusinessException(ErrorCode.WITHDRAWN_MEMBER)

		val receiver = memberRepository.findById(receiverId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		if (receiver.withdrawnAt != null || receiver.nickname == null) throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)

		if (blockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(memberId, receiverId, receiverId, memberId)) {
			throw BusinessException(ErrorCode.BLOCKED_MEMBER)
		}

		val existingRelations = friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(memberId, receiverId, receiverId, memberId)
		existingRelations.forEach { relation ->
			when {
				relation.status == FriendRequestStatus.ACCEPTED -> throw BusinessException(ErrorCode.ALREADY_FRIENDS)
				relation.status == FriendRequestStatus.PENDING && relation.requester.id == memberId -> throw BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS)
				relation.status == FriendRequestStatus.PENDING -> throw BusinessException(ErrorCode.REVERSE_FRIEND_REQUEST_EXISTS)
				// REJECTED rows don't block a new request; a fresh row is created below.
			}
		}

		val saved = try {
			friendRepository.saveAndFlush(Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.PENDING))
		} catch (e: DataIntegrityViolationException) {
			// Safety net for a concurrent reverse-direction PENDING insert that raced past the check above --
			// see uq_friend_pending_pair in V4__add_friend_request_indexes_and_pending_pair_constraint.sql.
			throw BusinessException(ErrorCode.REVERSE_FRIEND_REQUEST_EXISTS)
		}

		return SendFriendRequestResponse.from(saved)
	}
}
