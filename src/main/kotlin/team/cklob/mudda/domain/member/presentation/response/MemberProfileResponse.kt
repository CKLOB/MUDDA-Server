package team.cklob.mudda.domain.member.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.Gender
import java.time.LocalDateTime

@Schema(description = "다른 회원 프로필")
data class MemberProfileResponse(
	@Schema(description = "회원 ID", example = "2")
	val memberId: Long,
	@Schema(description = "닉네임", example = "nick", nullable = true)
	val nickname: String?,
	@Schema(description = "성별", example = "MALE", nullable = true)
	val gender: Gender?,
	@Schema(description = "출생 연도", example = "2008", nullable = true)
	val birthYear: Int?,
	@Schema(description = "프로필 이미지 URL", nullable = true)
	val profileImageUrl: String?,
	@Schema(description = "자기소개", nullable = true)
	val bio: String?,
	@Schema(description = "로그인 사용자와의 친구 관계 상태", example = "FRIEND")
	val friendStatus: FriendStatus,
	@Schema(description = "가입 시각")
	val createdAt: LocalDateTime,
) {
	companion object {
		fun of(member: Member, friendStatus: FriendStatus) = MemberProfileResponse(
			memberId = requireNotNull(member.id),
			nickname = member.nickname,
			gender = member.gender,
			birthYear = member.birthYear,
			profileImageUrl = member.profileImageUrl,
			bio = member.bio,
			friendStatus = friendStatus,
			createdAt = member.createdAt,
		)
	}
}
