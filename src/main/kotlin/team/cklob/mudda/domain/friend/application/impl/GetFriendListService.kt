package team.cklob.mudda.domain.friend.application.impl

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.presentation.response.FriendPageResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendResponse
import team.cklob.mudda.domain.member.domain.entity.Member

@Service
class GetFriendListService(
	private val friendRepository: FriendRepository,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, pageable: Pageable): FriendPageResponse<FriendResponse> {
		// Blocked counterparts are already excluded by FriendRepository#findFriendships itself (NOT EXISTS
		// in SQL), so the page's totalElements/totalPages/hasNext are accurate as-is -- no post-fetch
		// filtering needed here.
		val page = friendRepository.findFriendships(memberId, pageable)
		// accepted_at is backed by ck_friend_accepted_at (see V4 migration): the DB itself guarantees an
		// ACCEPTED row always has a non-null accepted_at, so this can never actually throw.
		val content = page.content.map { FriendResponse.of(counterpart(it, memberId), requireNotNull(it.acceptedAt)) }

		return FriendPageResponse.of(page, content)
	}

	private fun counterpart(friend: Friend, memberId: Long): Member = if (friend.requester.id == memberId) friend.receiver else friend.requester
}
