package team.cklob.mudda.domain.media.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "미디어 업로드 URL 발급 응답")
data class CreateMediaUploadUrlResponse(
	@Schema(description = "업로드 완료 등록 시 그대로 돌려보낼 키", example = "1/IMAGE/9f2c...")
	val uploadKey: String,
	@Schema(description = "이 URL에 파일을 PUT 하면 됩니다.")
	val uploadUrl: String,
	@Schema(description = "업로드 URL 만료 시각")
	val expiresAt: LocalDateTime,
)
