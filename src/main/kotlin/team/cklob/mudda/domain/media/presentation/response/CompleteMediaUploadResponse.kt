package team.cklob.mudda.domain.media.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.media.application.SignedUrl
import team.cklob.mudda.domain.media.domain.entity.Media
import team.cklob.mudda.domain.media.domain.type.MediaType
import java.time.LocalDateTime

@Schema(description = "미디어 업로드 완료 응답")
data class CompleteMediaUploadResponse(
	@Schema(description = "미디어 ID", example = "1")
	val mediaId: Long,
	@Schema(description = "조회용 Presigned URL")
	val accessUrl: String,
	@Schema(description = "조회 URL 만료 시각")
	val accessUrlExpiresAt: LocalDateTime,
	@Schema(description = "미디어 종류", example = "IMAGE")
	val mediaType: MediaType,
	@Schema(description = "등록 시각")
	val createdAt: LocalDateTime,
) {
	companion object {
		fun from(media: Media, accessUrl: SignedUrl) = CompleteMediaUploadResponse(
			mediaId = requireNotNull(media.id),
			accessUrl = accessUrl.url,
			accessUrlExpiresAt = accessUrl.expiresAt,
			mediaType = media.mediaType,
			createdAt = media.createdAt,
		)
	}
}
