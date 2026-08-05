package team.cklob.mudda.domain.friend.application.impl

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.presentation.response.FriendPageResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendResponse
import team.cklob.mudda.domain.member.domain.entity.Member

@Service
class GetFriendListService(
	private val friendRepository: FriendRepository,
	private val blockRepository: BlockRepository,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, pageable: Pageable): FriendPageResponse<FriendResponse> {
		val page = friendRepository.findFriendships(memberId, FriendRequestStatus.ACCEPTED, pageable)
		val blockedMemberIds = blockRepository.findByBlockerIdOrBlockedId(memberId, memberId)
			.mapNotNull { if (it.blocker.id == memberId) it.blocked.id else it.blocker.id }
			.toSet()

		// Block rows are filtered out of the already-paginated content, so a page can legitimately return
		// fewer than `size` items when a blocked member is among ACCEPTED friends -- acceptable for now since
		// blocking is expected to be rare and the Block domain's own API is out of this PR's scope.
		val content = page.content.mapNotNull { friend ->
			val other = counterpart(friend, memberId)
			if (other.id in blockedMemberIds) null else FriendResponse.of(other, requireNotNull(friend.acceptedAt))
		}

		return FriendPageResponse.of(page, content)
	}

	private fun counterpart(friend: Friend, memberId: Long): Member = if (friend.requester.id == memberId) friend.receiver else friend.requester
}
