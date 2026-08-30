package team.cklob.mudda.domain.timecapsule.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import java.time.LocalDateTime

@Schema(
	description = "타임캡슐 생성 요청. lockType에 따라 잠금 필드 조합이 정해집니다 — " +
		"NONE이면 password·question·answer가 모두 없어야 하고, PASSWORD면 password만, QUESTION이면 question과 answer가 함께 필요합니다.",
)
data class CreateCapsuleRequest(
	@field:NotBlank @field:Size(max = 255)
	@Schema(description = "캡슐 제목", example = "첫 캡슐")
	val name: String,

	@field:NotBlank
	@Schema(description = "캡슐 내용. 저장 시 AES-256-GCM으로 암호화됩니다.", example = "10년 뒤의 나에게")
	val content: String,

	@field:DecimalMin("-90.0") @field:DecimalMax("90.0")
	@Schema(description = "캡슐을 묻을 위도", example = "37.5")
	val latitude: Double,

	@field:DecimalMin("-180.0") @field:DecimalMax("180.0")
	@Schema(description = "캡슐을 묻을 경도", example = "127.0")
	val longitude: Double,

	@field:NotNull
	@Schema(description = "열람 가능해지는 시각. 현재보다 미래여야 합니다.", example = "2027-01-01T00:00:00")
	val openAt: LocalDateTime,

	@Schema(description = "만료 시각. openAt 이후여야 하며 최대 허용 연수를 넘을 수 없습니다.", example = "2030-01-01T00:00:00", nullable = true)
	val expiredAt: LocalDateTime? = null,

	@field:NotNull
	@Schema(description = "공개 범위", example = "PUBLIC")
	val visibility: CapsuleVisibility,

	@field:NotNull
	@Schema(description = "잠금 유형", example = "NONE")
	val lockType: CapsuleLockType,

	@Schema(description = "lockType이 PASSWORD일 때의 비밀번호", nullable = true)
	val password: String? = null,

	@field:Size(max = 255)
	@Schema(description = "lockType이 QUESTION일 때의 질문", example = "우리가 처음 만난 곳은?", nullable = true)
	val question: String? = null,

	@Schema(description = "lockType이 QUESTION일 때의 정답. 대소문자와 앞뒤 공백은 무시됩니다.", nullable = true)
	val answer: String? = null,

	@Schema(description = "캡슐을 받을 회원 ID 목록. 친구 관계이면서 차단되지 않은 회원이어야 합니다.", example = "[2, 3]")
	val recipientIds: Set<Long> = emptySet(),

	@Schema(description = "첨부할 미디어 ID 목록. 본인이 업로드했고 아직 다른 캡슐에 붙지 않은 것이어야 합니다.", example = "[10]")
	val mediaIds: Set<Long> = emptySet(),
)

@Schema(description = "캡슐 열람 요청. 좌표는 서버에서 PostGIS로 재검증합니다.")
data class OpenCapsuleRequest(
	@field:DecimalMin("-90.0") @field:DecimalMax("90.0")
	@Schema(description = "현재 위도", example = "37.5")
	val latitude: Double,

	@field:DecimalMin("-180.0") @field:DecimalMax("180.0")
	@Schema(description = "현재 경도", example = "127.0")
	val longitude: Double,

	@Schema(description = "lockType이 PASSWORD인 캡슐의 비밀번호. 최초 열람 시에만 검증합니다.", nullable = true)
	val password: String? = null,

	@Schema(description = "lockType이 QUESTION인 캡슐의 정답. 최초 열람 시에만 검증합니다.", nullable = true)
	val answer: String? = null,
)

@Schema(description = "방명록 작성 요청")
data class CreateGuestbookRequest(
	@field:NotBlank
	@Schema(description = "방명록 내용", example = "다녀갑니다")
	val content: String,
)

@Schema(description = "방명록 수정 요청")
data class UpdateGuestbookRequest(
	@field:NotBlank
	@Schema(description = "수정할 방명록 내용", example = "다시 다녀갑니다")
	val content: String,
)
