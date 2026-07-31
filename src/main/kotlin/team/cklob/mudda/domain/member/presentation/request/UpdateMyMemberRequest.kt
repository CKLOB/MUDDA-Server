package team.cklob.mudda.domain.member.presentation.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility

data class UpdateMyMemberRequest(
	@field:Size(max = 30)
	val name: String? = null,

	@field:Size(max = 30)
	val nickname: String? = null,

	val gender: Gender? = null,

	@field:Min(1900)
	@field:Max(2100)
	val birthYear: Int? = null,

	@field:Size(max = 255)
	val profileImageUrl: String? = null,

	@field:Size(max = 100)
	val bio: String? = null,

	val profileVisibility: ProfileVisibility? = null,
) {
	// Add new fields to this comparison too, or an all-null request for the new field would silently pass.
	fun isEmpty(): Boolean =
		name == null && nickname == null && gender == null && birthYear == null &&
			profileImageUrl == null && bio == null && profileVisibility == null
}
