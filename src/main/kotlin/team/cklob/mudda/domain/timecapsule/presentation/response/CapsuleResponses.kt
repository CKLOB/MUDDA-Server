package team.cklob.mudda.domain.timecapsule.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.media.domain.type.MediaType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleEncryptionMode
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

@Schema(description = "CEK 조각 하나")
data class KeyShareResponse(
	@Schema(description = "Shamir x 좌표", example = "1") val index: Int,
	@Schema(description = "조각 데이터(Base64)") val data: String,
	@Schema(
		description = "잠금 비밀에서 유도한 키로 감싸진 조각인지 여부. true이면 클라이언트가 비밀번호·정답으로 먼저 풀어야 합니다.",
		example = "true",
	)
	val isWrapped: Boolean,
)

@Schema(
	description = "캡슐 열람 응답. encryptionMode에 따라 채워지는 필드가 다릅니다 — " +
		"SERVER_ENVELOPE이면 content에 평문이 담기고, CLIENT_E2E이면 content는 비어 있고 " +
		"contentCipher와 keyShares로 클라이언트가 직접 복호화해야 합니다.",
)
data class OpenCapsuleResponse(
	@Schema(description = "캡슐 ID", example = "1") val capsuleId: Long,
	@Schema(description = "캡슐 제목", example = "첫 캡슐") val title: String,
	@Schema(description = "암호화 모드", example = "CLIENT_E2E") val encryptionMode: CapsuleEncryptionMode,
	@Schema(description = "평문 내용. SERVER_ENVELOPE 캡슐에서만 채워집니다.", nullable = true) val content: String?,
	@Schema(description = "클라이언트가 복호화해야 할 blob. CLIENT_E2E 캡슐에서만 채워집니다.", nullable = true)
	val contentCipher: String?,
	@Schema(description = "서버가 보관하던 CEK 조각들. CLIENT_E2E 캡슐에서만 채워집니다.")
	val keyShares: List<KeyShareResponse>,
	@Schema(description = "CEK 복원에 필요한 조각 수. CLIENT_E2E 캡슐에서만 채워집니다.", example = "2", nullable = true)
	val keyThreshold: Int?,
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
