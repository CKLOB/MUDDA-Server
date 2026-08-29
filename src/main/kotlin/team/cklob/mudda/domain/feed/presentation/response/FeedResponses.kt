package team.cklob.mudda.domain.feed.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.feed.domain.type.FeedType
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleOpen
import java.time.LocalDateTime

@Schema(description = "피드에 표시되는 사용자")
data class FeedMemberResponse(
	@Schema(description = "회원 ID", example = "1")
	val memberId: Long,

	@Schema(description = "닉네임", example = "nick", nullable = true)
	val nickname: String?,

	// The spec models this as profileMedia { mediaId, url, type }, but every shipped endpoint
	// (MemberProfileResponse, WriterResponse, ...) exposes Member.profileImageUrl, and Member has no media
	// FK to build the object from. Kept consistent with the rest of the API rather than reshaping the
	// profile model from inside the feed domain.
	@Schema(description = "프로필 이미지 URL", nullable = true)
	val profileImageUrl: String?,
) {
	companion object {
		fun from(member: Member) = FeedMemberResponse(
			memberId = requireNotNull(member.id),
			nickname = member.nickname,
			profileImageUrl = member.profileImageUrl,
		)
	}
}

@Schema(description = "발견 피드 항목")
data class FeedResponse(
	@Schema(description = "피드 ID", example = "1")
	val feedId: Long,

	@Schema(description = "피드 종류", example = "CAPSULE_OPENED")
	val type: FeedType,

	@Schema(description = "피드 문구", example = "nick님이 '첫 캡슐'을(를) 발견했어요.")
	val message: String,

	@Schema(description = "피드를 발생시킨 사용자")
	val member: FeedMemberResponse,

	@Schema(description = "관련 캡슐 ID", example = "12")
	val capsuleId: Long,

	@Schema(description = "발생 시각")
	val createdAt: LocalDateTime,
) {
	companion object {
		fun from(open: CapsuleOpen): FeedResponse {
			val member = open.member
			val capsule = open.timeCapsule
			return FeedResponse(
				feedId = requireNotNull(open.id),
				type = FeedType.CAPSULE_OPENED,
				message = "${member.nickname ?: "누군가"}님이 '${capsule.name}'을(를) 발견했어요.",
				member = FeedMemberResponse.from(member),
				capsuleId = requireNotNull(capsule.id),
				createdAt = open.openedAt,
			)
		}
	}
}

@Schema(description = "발견 피드 목록 응답")
data class FeedListResponse(
	@Schema(description = "피드 목록")
	val feeds: List<FeedResponse>,
)
