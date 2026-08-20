package team.cklob.mudda.domain.timecapsule.application

import org.springframework.stereotype.Component
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleRecipientRepository
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.global.exception.CapsuleException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime

@Component
class CapsuleAccessPolicy(
	private val recipientRepository: CapsuleRecipientRepository,
	private val friendRepository: FriendRepository,
	private val blockRepository: BlockRepository,
) {
	fun canAccess(capsule: TimeCapsule, memberId: Long): Boolean {
		val writerId = requireNotNull(capsule.member.id)
		if (writerId == memberId) return true
		if (blockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(memberId, writerId, writerId, memberId)) return false
		if (recipientRepository.existsByTimeCapsuleIdAndMemberId(requireNotNull(capsule.id), memberId)) return true
		return when (capsule.visibility) {
			CapsuleVisibility.PRIVATE -> false
			CapsuleVisibility.FRIEND -> friendRepository.existsAcceptedBetween(memberId, writerId)
			CapsuleVisibility.PUBLIC -> true
		}
	}

	fun requireAccessible(capsule: TimeCapsule, memberId: Long, now: LocalDateTime = LocalDateTime.now()) {
		if (capsule.isDeleted) throw CapsuleException(ErrorCode.CAPSULE_NOT_FOUND)
		if (capsule.expiredAt?.let { !it.isAfter(now) } == true) throw CapsuleException(ErrorCode.CAPSULE_EXPIRED)
		if (!canAccess(capsule, memberId)) throw CapsuleException(ErrorCode.CAPSULE_ACCESS_DENIED)
	}
}
