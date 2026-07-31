package team.cklob.mudda.domain.member.presentation.response

import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import java.time.LocalDateTime

data class MyMemberResponse(
	val memberId: Long,
	val name: String?,
	val nickname: String?,
	val gender: Gender?,
	val birthYear: Int?,
	val profileImageUrl: String?,
	val bio: String?,
	val profileVisibility: ProfileVisibility,
	val createdAt: LocalDateTime,
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
