package team.cklob.mudda.domain.media.presentation.response

import team.cklob.mudda.domain.media.application.SignedUrl
import team.cklob.mudda.domain.media.domain.entity.Media
import team.cklob.mudda.domain.media.domain.type.MediaType
import java.time.LocalDateTime

data class CompleteMediaUploadResponse(
	val mediaId: Long,
	val accessUrl: String,
	val accessUrlExpiresAt: LocalDateTime,
	val mediaType: MediaType,
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
