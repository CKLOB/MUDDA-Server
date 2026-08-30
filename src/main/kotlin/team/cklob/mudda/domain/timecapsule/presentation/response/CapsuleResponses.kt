package team.cklob.mudda.domain.timecapsule.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.media.domain.type.MediaType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import java.time.LocalDateTime

@Schema(description = "캡슐 작성자")
data class WriterResponse(
	@Schema(description = "회원 ID", example = "1") val memberId: Long,
	@Schema(description = "닉네임", example = "nick", nullable = true) val nickname: String?,
	@Schema(description = "프로필 이미지 URL", nullable = true) val profileImageUrl: String?,
)

@Schema(description = "캡슐에 첨부된 미디어")
data class MediaResponse(
	@Schema(description = "미디어 ID", example = "10") val mediaId: Long,
	@Schema(description = "조회용 Presigned URL") val url: String,
	@Schema(description = "미디어 종류", example = "IMAGE") val type: MediaType,
)

@Schema(description = "타임캡슐 생성 응답")
data class CreateCapsuleResponse(
	@Schema(description = "캡슐 ID", example = "1") val capsuleId: Long,
	@Schema(description = "캡슐 제목", example = "첫 캡슐") val title: String,
	@Schema(description = "위도", example = "37.5") val latitude: Double,
	@Schema(description = "경도", example = "127.0") val longitude: Double,
	@Schema(description = "열람 가능 시각") val openAt: LocalDateTime,
	@Schema(description = "만료 시각", nullable = true) val expiredAt: LocalDateTime?,
	@Schema(description = "생성 시각") val createdAt: LocalDateTime,
)

@Schema(description = "타임캡슐 목록 항목")
data class CapsuleListItemResponse(
	@Schema(description = "캡슐 ID", example = "1") val capsuleId: Long,
	@Schema(description = "캡슐 제목", example = "첫 캡슐") val title: String,
	@Schema(description = "작성자. 내가 만든 캡슐 목록에서는 생략됩니다.", nullable = true) val writer: WriterResponse?,
	@Schema(description = "위도", example = "37.5") val latitude: Double,
	@Schema(description = "경도", example = "127.0") val longitude: Double,
	@Schema(description = "열람 가능 시각") val openAt: LocalDateTime,
	@Schema(description = "만료 시각", nullable = true) val expiredAt: LocalDateTime?,
	@Schema(description = "공개 범위", example = "PUBLIC") val visibility: CapsuleVisibility,
	@Schema(description = "열람에 비밀번호가 필요한지 여부", example = "false") val requiresPassword: Boolean,
	@Schema(description = "열람에 질문 응답이 필요한지 여부", example = "false") val requiresAnswer: Boolean,
	@Schema(description = "로그인 사용자가 이미 열어본 캡슐인지 여부", example = "false") val isOpened: Boolean,
	@Schema(description = "생성 시각") val createdAt: LocalDateTime,
)

@Schema(description = "주변 타임캡슐 항목")
data class NearbyCapsuleResponse(
	@Schema(description = "캡슐 ID", example = "1") val capsuleId: Long,
	@Schema(description = "캡슐 제목", example = "첫 캡슐") val title: String,
	@Schema(description = "위도", example = "37.5") val latitude: Double,
	@Schema(description = "경도", example = "127.0") val longitude: Double,
	@Schema(description = "현재 위치로부터의 거리(미터)", example = "42.7") val distance: Double,
	@Schema(description = "열람하려면 들어가야 하는 반경(미터)", example = "100.0") val requiredDistance: Double,
	@Schema(description = "열람 가능 시각") val openAt: LocalDateTime,
	@Schema(description = "잠금 유형", example = "NONE") val lockType: CapsuleLockType,
	@Schema(description = "로그인 사용자가 이미 열어본 캡슐인지 여부", example = "false") val isOpened: Boolean,
)

@Schema(description = "타임캡슐 상세. 내용은 포함되지 않으며 열람 API로만 얻을 수 있습니다.")
data class CapsuleDetailResponse(
	@Schema(description = "캡슐 ID", example = "1") val capsuleId: Long,
	@Schema(description = "캡슐 제목", example = "첫 캡슐") val title: String,
	@Schema(description = "작성자") val writer: WriterResponse,
	@Schema(description = "위도", example = "37.5") val latitude: Double,
	@Schema(description = "경도", example = "127.0") val longitude: Double,
	@Schema(description = "열람하려면 들어가야 하는 반경(미터)", example = "100.0") val requiredDistance: Double,
	@Schema(description = "열람 가능 시각") val openAt: LocalDateTime,
	@Schema(description = "만료 시각", nullable = true) val expiredAt: LocalDateTime?,
	@Schema(description = "공개 범위", example = "PUBLIC") val visibility: CapsuleVisibility,
	@Schema(description = "잠금 유형", example = "QUESTION") val lockType: CapsuleLockType,
	@Schema(description = "lockType이 QUESTION일 때의 질문", nullable = true) val question: String?,
	@Schema(description = "로그인 사용자가 이미 열어본 캡슐인지 여부", example = "false") val isOpened: Boolean,
	@Schema(description = "생성 시각") val createdAt: LocalDateTime,
	@Schema(description = "최종 수정 시각") val updatedAt: LocalDateTime,
)

@Schema(description = "캡슐 열람 응답")
data class OpenCapsuleResponse(
	@Schema(description = "캡슐 ID", example = "1") val capsuleId: Long,
	@Schema(description = "캡슐 제목", example = "첫 캡슐") val title: String,
	@Schema(description = "복호화된 캡슐 내용") val content: String,
	@Schema(description = "작성자") val writer: WriterResponse,
	@Schema(description = "첨부 미디어 목록") val media: List<MediaResponse>,
	@Schema(description = "최초 열람 시각. 재열람해도 갱신되지 않습니다.") val openedAt: LocalDateTime,
)

@Schema(description = "타임캡슐 페이지 응답")
data class CapsulePageResponse<T>(
	@Schema(description = "캡슐 목록") val capsules: List<T>,
	@Schema(description = "현재 페이지 번호(0-base)", example = "0") val page: Int,
	@Schema(description = "페이지 크기", example = "20") val size: Int,
	@Schema(description = "전체 캡슐 수", example = "42") val totalCount: Long,
)

@Schema(description = "방명록")
data class GuestbookResponse(
	@Schema(description = "방명록 ID", example = "1") val guestbookId: Long,
	@Schema(description = "캡슐 ID", example = "1") val capsuleId: Long,
	@Schema(description = "작성자") val writer: WriterResponse,
	@Schema(description = "방명록 내용", example = "다녀갑니다") val content: String,
	@Schema(description = "작성 시각") val createdAt: LocalDateTime,
	@Schema(description = "최종 수정 시각") val updatedAt: LocalDateTime,
)

@Schema(description = "방명록 페이지 응답")
data class GuestbookPageResponse(
	@Schema(description = "방명록 목록") val guestbooks: List<GuestbookResponse>,
	@Schema(description = "현재 페이지 번호(0-base)", example = "0") val page: Int,
	@Schema(description = "페이지 크기", example = "20") val size: Int,
	@Schema(description = "전체 방명록 수", example = "5") val totalCount: Long,
)

@Schema(description = "방명록 수정 응답")
data class UpdateGuestbookResponse(
	@Schema(description = "방명록 ID", example = "1") val guestbookId: Long,
	@Schema(description = "수정된 내용", example = "다시 다녀갑니다") val content: String,
	@Schema(description = "수정 시각") val updatedAt: LocalDateTime,
)
