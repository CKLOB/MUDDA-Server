package team.cklob.mudda.domain.member.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import java.time.LocalDateTime

@Schema(description = "내 정보")
data class MyMemberResponse(
	@Schema(description = "회원 ID", example = "1")
	val memberId: Long,
	@Schema(description = "실명", example = "박하민", nullable = true)
	val name: String?,
	@Schema(description = "닉네임", example = "hamin", nullable = true)
	val nickname: String?,
	@Schema(description = "성별", example = "MALE", nullable = true)
	val gender: Gender?,
	@Schema(description = "출생 연도", example = "2008", nullable = true)
	val birthYear: Int?,
	@Schema(description = "프로필 이미지 URL", nullable = true)
	val profileImageUrl: String?,
	@Schema(description = "자기소개", nullable = true)
	val bio: String?,
	@Schema(description = "프로필 공개 범위", example = "PUBLIC")
	val profileVisibility: ProfileVisibility,
	@Schema(description = "가입 시각")
	val createdAt: LocalDateTime,
	@Schema(description = "최종 수정 시각")
	val updatedAt: LocalDateTime,
) {
	companion object {
		fun from(member: Member) = MyMemberResponse(
			memberId = requireNotNull(member.id),
			name = member.name,
			nickname = member.nickname,
			gender = member.gender,
			birthYear = member.birthYear,
			profileImageUrl = member.profileImageUrl,
			bio = member.bio,
			profileVisibility = member.profileVisibility,
			createdAt = member.createdAt,
			updatedAt = member.updatedAt,
		)
	}
}
