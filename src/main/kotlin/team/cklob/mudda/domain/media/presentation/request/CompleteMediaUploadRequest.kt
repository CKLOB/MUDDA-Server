package team.cklob.mudda.domain.media.presentation.request

import jakarta.validation.constraints.NotBlank

data class CompleteMediaUploadRequest(
	@field:NotBlank val uploadKey: String,
)
