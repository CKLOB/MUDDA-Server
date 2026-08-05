package team.cklob.mudda.domain.friend.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

// receiverId is nullable + @NotNull (rather than a bare non-null Long) so a missing/explicit-null value
// fails bean validation with a 400 before reaching the service, instead of relying on Jackson's
// implicit Kotlin non-null parameter enforcement for a primitive-backed type, which does not reliably
// trigger for a missing JSON key.
@Schema(description = "친구 요청 전송 요청")
data class SendFriendRequestRequest(
	@field:NotNull
	@Schema(description = "친구 요청을 받을 회원의 ID", example = "2")
	val receiverId: Long?,
)
