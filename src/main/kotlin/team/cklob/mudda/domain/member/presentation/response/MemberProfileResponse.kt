package team.cklob.mudda.domain.member.presentation.response

import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.Gender
import java.time.LocalDateTime

data class MemberProfileResponse(
	val memberId: Long,
	val name: String?,
	val nickname: String?,
	val gender: Gender?,
	val birthYear: Int?,
	val profileImageUrl: String?,
	val bio: String?,
	val friendStatus: FriendStatus,
	val createdAt: LocalDateTime,
) {
	companion object {
		fun of(member: Member, friendStatus: FriendStatus) = MemberProfileResponse(
			memberId = requireNotNull(member.id),
			name = member.name,
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
