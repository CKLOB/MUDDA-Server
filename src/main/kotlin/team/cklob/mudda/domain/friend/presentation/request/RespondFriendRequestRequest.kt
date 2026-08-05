package team.cklob.mudda.domain.friend.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.friend.domain.type.FriendRequestAction

@Schema(description = "친구 요청 수락/거절 요청")
data class RespondFriendRequestRequest(
	@Schema(description = "수행할 행위. ACCEPT 또는 REJECT", example = "ACCEPT")
	val action: FriendRequestAction,
)
