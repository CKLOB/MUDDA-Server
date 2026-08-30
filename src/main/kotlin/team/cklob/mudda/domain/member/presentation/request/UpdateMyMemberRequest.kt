package team.cklob.mudda.domain.member.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility

@Schema(description = "내 정보 수정 요청. 전달한 필드만 수정되며, 모두 생략하면 400을 반환합니다.")
data class UpdateMyMemberRequest(
	@field:Size(max = 30)
	@Schema(description = "실명", example = "박하민", nullable = true)
	val name: String? = null,

	@field:Size(max = 30)
	@Schema(description = "닉네임. 전체에서 유일해야 합니다.", example = "hamin", nullable = true)
	val nickname: String? = null,

	@Schema(description = "성별", example = "MALE", nullable = true)
	val gender: Gender? = null,

	@field:Min(1900)
	@field:Max(2100)
	@Schema(description = "출생 연도", example = "2008", nullable = true)
	val birthYear: Int? = null,

	// Blank is allowed through here so the service layer's empty-string-to-null clearing still works;
	// only an actually non-blank, non-http(s) value (e.g. javascript:, data:, file:) is rejected.
	@field:Pattern(regexp = "^\\s*$|^https?://\\S+$", message = "profileImageUrl must be blank or an http(s) URL")
	@field:Size(max = 255)
	@Schema(description = "프로필 이미지 URL. 빈 문자열을 보내면 기존 이미지가 제거됩니다.", example = "https://cdn.mudda.app/p/1.png", nullable = true)
	val profileImageUrl: String? = null,

	@field:Size(max = 100)
	@Schema(description = "자기소개", example = "타임캡슐 좋아합니다", nullable = true)
	val bio: String? = null,

	@Schema(description = "프로필 공개 범위", example = "PUBLIC", nullable = true)
	val profileVisibility: ProfileVisibility? = null,
) {
	// Add new fields to this comparison too, or an all-null request for the new field would silently pass.
	fun isEmpty(): Boolean =
		name == null && nickname == null && gender == null && birthYear == null &&
			profileImageUrl == null && bio == null && profileVisibility == null
}
