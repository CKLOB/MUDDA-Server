package team.cklob.mudda.domain.timecapsule.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
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

	@Schema(
		description = "평문 캡슐 내용. lockType이 NONE일 때만 사용하며, 서버가 AES-256-GCM으로 암호화해 보관합니다. " +
			"잠금이 있는 캡슐에서는 서버가 평문을 받아서는 안 되므로 생략하고 contentCipher를 보내야 합니다.",
		example = "10년 뒤의 나에게",
		nullable = true,
	)
	val content: String? = null,

	@Schema(
		description = "클라이언트가 CEK로 암호화한 본문 blob(Base64). lockType이 PASSWORD 또는 QUESTION일 때 필수입니다. " +
			"서버는 이 값을 해독할 수 없습니다.",
		nullable = true,
	)
	val contentCipher: String? = null,

	@Schema(
		description = "CEK를 Shamir로 분할한 조각 중 서버에 맡길 것들. lockType이 PASSWORD 또는 QUESTION일 때 필수이며, " +
			"복원 임계값보다 적은 수의 평문 조각만 포함해야 합니다. 잠금 비밀에서 유도한 키로 감싼 조각은 isWrapped=true로 표시합니다.",
		nullable = true,
	)
	val keyShares: List<KeyShareRequest>? = null,

	@Schema(
		description = "CEK 복원에 필요한 조각 수(Shamir 임계값). lockType이 PASSWORD 또는 QUESTION일 때 필수입니다. " +
			"서버는 보관하는 평문 조각 수가 이 값보다 적은지 검증하며, 그렇지 않으면 요청을 거부합니다.",
		example = "2",
		nullable = true,
	)
	@field:Min(2) @field:Max(255)
	val keyThreshold: Int? = null,

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

@Schema(description = "서버에 보관할 CEK 조각 하나")
data class KeyShareRequest(
	@field:NotNull
	@field:Min(1) @field:Max(255)
	@Schema(description = "Shamir x 좌표. 복원에 반드시 필요하므로 조각과 함께 보관됩니다.", example = "1")
	val index: Int?,

	@field:NotBlank
	@Schema(description = "조각 데이터(Base64)", example = "q83vASNFZ4k=")
	val data: String?,

	@field:NotNull
	@Schema(
		description = "잠금 비밀에서 유도한 키로 감싼 조각인지 여부. true인 조각은 서버가 풀 수 없어 서버의 정족수에 포함되지 않습니다.",
		example = "false",
	)
	val isWrapped: Boolean?,
)
