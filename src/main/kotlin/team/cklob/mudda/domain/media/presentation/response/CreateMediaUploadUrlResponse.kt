package team.cklob.mudda.domain.media.presentation.response

import java.time.LocalDateTime

data class CreateMediaUploadUrlResponse(
	val uploadKey: String,
	val uploadUrl: String,
	val expiresAt: LocalDateTime,
)
