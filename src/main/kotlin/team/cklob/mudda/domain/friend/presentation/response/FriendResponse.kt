package team.cklob.mudda.domain.friend.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.member.domain.entity.Member
import java.time.LocalDateTime

@Schema(description = "친구 목록 항목")
data class FriendResponse(
	@Schema(description = "친구의 회원 ID", example = "2")
	val memberId: Long,

	@Schema(description = "친구의 닉네임", example = "nickname")
	val nickname: String?,

	@Schema(description = "친구의 프로필 이미지 URL", example = "https://cdn.mudda.team/profile/2.png")
	val profileImageUrl: String?,

	@Schema(description = "친구가 된 시각")
	val acceptedAt: LocalDateTime,
) {
	companion object {
		fun of(counterpart: Member, acceptedAt: LocalDateTime) = FriendResponse(
			memberId = requireNotNull(counterpart.id),
			nickname = counterpart.nickname,
			profileImageUrl = counterpart.profileImageUrl,
			acceptedAt = acceptedAt,
		)
	}
}
