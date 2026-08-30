package team.cklob.mudda.domain.media.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "미디어 업로드 완료 등록 요청")
data class CompleteMediaUploadRequest(
	@field:NotBlank
	@Schema(description = "업로드 URL 발급 시 함께 받은 업로드 키", example = "1/IMAGE/9f2c...")
	val uploadKey: String,
)
