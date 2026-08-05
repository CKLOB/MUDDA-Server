package team.cklob.mudda.domain.friend.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.domain.type.FriendRequestType
import team.cklob.mudda.domain.member.domain.entity.Member
import java.time.LocalDateTime

@Schema(description = "친구 요청 목록 항목")
data class FriendRequestResponse(
	@Schema(description = "친구 요청(Friend row)의 ID", example = "10")
	val requestId: Long,

	@Schema(description = "요청 방향. RECEIVED(내가 받음) 또는 SENT(내가 보냄)", example = "RECEIVED")
	val direction: FriendRequestType,

	@Schema(description = "상대방의 회원 ID", example = "2")
	val memberId: Long,

	@Schema(description = "상대방의 닉네임", example = "nickname")
	val nickname: String?,

	@Schema(description = "상대방의 프로필 이미지 URL", example = "https://cdn.mudda.team/profile/2.png")
	val profileImageUrl: String?,

	@Schema(description = "요청 생성 시각")
	val createdAt: LocalDateTime,

	@Schema(description = "요청 상태", example = "PENDING")
	val status: FriendRequestStatus,
) {
	companion object {
		fun of(friend: Friend, direction: FriendRequestType, counterpart: Member) = FriendRequestResponse(
			requestId = requireNotNull(friend.id),
			direction = direction,
			memberId = requireNotNull(counterpart.id),
			nickname = counterpart.nickname,
			profileImageUrl = counterpart.profileImageUrl,
			createdAt = friend.createdAt,
			status = friend.status,
		)
	}
}
