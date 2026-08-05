package team.cklob.mudda.domain.friend.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus

@Schema(description = "친구 요청 전송 결과")
data class SendFriendRequestResponse(
	@Schema(description = "생성된 친구 요청(Friend row)의 ID", example = "10")
	val requestId: Long,

	@Schema(description = "생성된 요청의 상태", example = "PENDING")
	val status: FriendRequestStatus,
) {
	companion object {
		fun from(friend: Friend) = SendFriendRequestResponse(requestId = requireNotNull(friend.id), status = friend.status)
	}
}
