package team.cklob.mudda.domain.friend.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.friend.domain.type.FriendRequestType
import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.domain.entity.Member

@Schema(description = "사용자 검색 결과 항목")
data class FriendSearchResponse(
	@Schema(description = "검색된 회원의 ID", example = "2")
	val memberId: Long,

	@Schema(description = "검색된 회원의 닉네임", example = "nickname")
	val nickname: String?,

	@Schema(description = "검색된 회원의 프로필 이미지 URL", example = "https://cdn.mudda.team/profile/2.png")
	val profileImageUrl: String?,

	@Schema(description = "로그인 사용자와의 관계 상태. NONE / FRIEND / REQUESTED(내가 보냄) / RECEIVED(내가 받음)", example = "NONE")
	val relationStatus: FriendStatus,

	@Schema(description = "진행 중이거나 성사된 친구 관계 row의 ID. 관계가 없으면 null", example = "null")
	val requestId: Long?,

	@Schema(description = "관계를 먼저 시작한 방향. 관계가 없으면 null", example = "null")
	val requestDirection: FriendRequestType?,
) {
	companion object {
		fun of(candidate: Member, relationStatus: FriendStatus, requestId: Long?, requestDirection: FriendRequestType?) = FriendSearchResponse(
			memberId = requireNotNull(candidate.id),
			nickname = candidate.nickname,
			profileImageUrl = candidate.profileImageUrl,
			relationStatus = relationStatus,
			requestId = requestId,
			requestDirection = requestDirection,
		)
	}
}
