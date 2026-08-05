package team.cklob.mudda.domain.friend.application.impl

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.domain.type.FriendRequestType
import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.friend.presentation.response.FriendPageResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendSearchResponse
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class SearchFriendService(
	private val memberRepository: MemberRepository,
	private val friendRepository: FriendRepository,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, keyword: String, pageable: Pageable): FriendPageResponse<FriendSearchResponse> {
		val trimmed = keyword.trim()
		if (trimmed.isBlank()) throw BusinessException(ErrorCode.INVALID_SEARCH_KEYWORD)

		val page = memberRepository.searchSelectableByNickname(memberId, trimmed, pageable)
		val candidateIds = page.content.mapNotNull { it.id }
		val relationsByOtherId = if (candidateIds.isEmpty()) emptyMap() else groupRelationsByOtherId(memberId, friendRepository.findAllBetween(memberId, candidateIds))

		val content = page.content.map { candidate ->
			val relation = relationsByOtherId[candidate.id]
			val (status, direction) = resolveRelation(memberId, relation)
			FriendSearchResponse.of(candidate, status, relation?.id, direction)
		}

		return FriendPageResponse.of(page, content)
	}

	// A requester/receiver pair can have relationship rows in both directions (see FriendRepository), so an
	// ACCEPTED row always wins over a stray PENDING row for the same pair, mirroring GetMemberProfileService.
	private fun groupRelationsByOtherId(memberId: Long, relations: List<Friend>): Map<Long, Friend> =
		relations.groupBy { if (it.requester.id == memberId) it.receiver.id else it.requester.id }
			.mapNotNull { (otherId, rels) ->
				val chosen = rels.firstOrNull { it.status == FriendRequestStatus.ACCEPTED } ?: rels.firstOrNull { it.status == FriendRequestStatus.PENDING } ?: rels.first()
				otherId?.let { it to chosen }
			}.toMap()

	private fun resolveRelation(memberId: Long, relation: Friend?): Pair<FriendStatus, FriendRequestType?> {
		if (relation == null) return FriendStatus.NONE to null
		val sentByMe = relation.requester.id == memberId
		return when (relation.status) {
			FriendRequestStatus.ACCEPTED -> FriendStatus.FRIEND to (if (sentByMe) FriendRequestType.SENT else FriendRequestType.RECEIVED)
			FriendRequestStatus.PENDING -> (if (sentByMe) FriendStatus.REQUESTED else FriendStatus.RECEIVED) to (if (sentByMe) FriendRequestType.SENT else FriendRequestType.RECEIVED)
			FriendRequestStatus.REJECTED -> FriendStatus.NONE to null
		}
	}
}
