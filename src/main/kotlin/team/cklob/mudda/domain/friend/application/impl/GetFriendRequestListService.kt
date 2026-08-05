package team.cklob.mudda.domain.friend.application.impl

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.domain.type.FriendRequestType
import team.cklob.mudda.domain.friend.presentation.response.FriendPageResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendRequestResponse

@Service
class GetFriendRequestListService(
	private val friendRepository: FriendRepository,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, type: FriendRequestType, status: FriendRequestStatus, pageable: Pageable): FriendPageResponse<FriendRequestResponse> {
		val page: Page<Friend> = when (type) {
			FriendRequestType.RECEIVED -> friendRepository.findReceivedRequests(memberId, status, pageable)
			FriendRequestType.SENT -> friendRepository.findSentRequests(memberId, status, pageable)
		}

		val content = page.content.map { friend ->
			val counterpart = if (type == FriendRequestType.RECEIVED) friend.requester else friend.receiver
			FriendRequestResponse.of(friend, type, counterpart)
		}

		return FriendPageResponse.of(page, content)
	}
}
