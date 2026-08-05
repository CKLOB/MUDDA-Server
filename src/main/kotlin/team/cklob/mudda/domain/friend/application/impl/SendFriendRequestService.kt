package team.cklob.mudda.domain.friend.application.impl

import org.slf4j.LoggerFactory
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
	private val logger = LoggerFactory.getLogger(javaClass)

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

		// Direction matters here: if I blocked them, telling them BLOCKED_MEMBER doesn't leak anything they
		// don't already know. If they blocked me, BLOCKED_MEMBER would leak the fact that a block exists
		// (unlike the search API, which silently excludes blocked members via a NOT EXISTS filter) -- so
		// that direction is reported as MEMBER_NOT_FOUND instead, indistinguishable from a nonexistent id.
		if (blockRepository.existsByBlockerIdAndBlockedId(memberId, receiverId)) {
			throw BusinessException(ErrorCode.BLOCKED_MEMBER)
		}
		if (blockRepository.existsByBlockerIdAndBlockedId(receiverId, memberId)) {
			throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
		}

		val existingRelations = friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(memberId, receiverId, receiverId, memberId)
		existingRelations.forEach { relation ->
			when {
				relation.status == FriendRequestStatus.ACCEPTED -> throw BusinessException(ErrorCode.ALREADY_FRIENDS)
				relation.status == FriendRequestStatus.PENDING && relation.requester.id == memberId -> throw BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS)
				relation.status == FriendRequestStatus.PENDING -> throw BusinessException(ErrorCode.REVERSE_FRIEND_REQUEST_EXISTS)
				// A REJECTED row doesn't block a new request -- uq_friend_requester_receiver (see V4) is a
				// partial index that excludes REJECTED rows, so a fresh row for the same direction can be
				// inserted below even while the old REJECTED row is kept around as history.
			}
		}

		val saved = try {
			friendRepository.saveAndFlush(Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.PENDING))
		} catch (e: DataIntegrityViolationException) {
			// Safety net for a concurrent insert that raced past the checks above -- most likely the reverse-
			// direction pending race guarded by uq_friend_pending_pair, but could in principle be any
			// constraint on this table (e.g. a member row deleted mid-request). Logged with the original
			// exception since folding every violation into one error code would otherwise hide the real cause.
			logger.warn("friend request insert violated a constraint: requester={}, receiver={}", memberId, receiverId, e)
			throw BusinessException(ErrorCode.REVERSE_FRIEND_REQUEST_EXISTS)
		}

		return SendFriendRequestResponse.from(saved)
	}
}
