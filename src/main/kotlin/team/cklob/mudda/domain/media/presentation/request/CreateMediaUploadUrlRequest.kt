package team.cklob.mudda.domain.media.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import team.cklob.mudda.domain.media.domain.type.MediaType

@Schema(description = "미디어 업로드 URL 발급 요청")
data class CreateMediaUploadUrlRequest(
	@Schema(description = "미디어 종류", example = "IMAGE")
	val mediaType: MediaType,
	@field:NotBlank
	@Schema(description = "업로드할 파일의 Content-Type. 종류별 허용 목록이 있습니다.", example = "image/jpeg")
	val contentType: String,
	@field:Positive
	@Schema(description = "파일 크기(바이트). 종류별 최대 용량을 넘을 수 없습니다.", example = "204800")
	val fileSize: Long,
)
