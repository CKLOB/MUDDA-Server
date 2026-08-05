package team.cklob.mudda.domain.media.presentation.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import team.cklob.mudda.domain.media.domain.type.MediaType

data class CreateMediaUploadUrlRequest(
	val mediaType: MediaType,
	@field:NotBlank val contentType: String,
	@field:Positive val fileSize: Long,
)
