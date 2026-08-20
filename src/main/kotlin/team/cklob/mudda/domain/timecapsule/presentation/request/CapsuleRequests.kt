package team.cklob.mudda.domain.timecapsule.presentation.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import java.time.LocalDateTime

data class CreateCapsuleRequest(
	@field:NotBlank @field:Size(max = 255) val name: String,
	@field:NotBlank val content: String,
	@field:DecimalMin("-90.0") @field:DecimalMax("90.0") val latitude: Double,
	@field:DecimalMin("-180.0") @field:DecimalMax("180.0") val longitude: Double,
	@field:NotNull val openAt: LocalDateTime,
	val expiredAt: LocalDateTime? = null,
	@field:NotNull val visibility: CapsuleVisibility,
	@field:NotNull val lockType: CapsuleLockType,
	val password: String? = null,
	@field:Size(max = 255) val question: String? = null,
	val answer: String? = null,
	val recipientIds: Set<Long> = emptySet(),
	val mediaIds: Set<Long> = emptySet(),
)

data class OpenCapsuleRequest(
	@field:DecimalMin("-90.0") @field:DecimalMax("90.0") val latitude: Double,
	@field:DecimalMin("-180.0") @field:DecimalMax("180.0") val longitude: Double,
	val password: String? = null,
	val answer: String? = null,
)

data class CreateGuestbookRequest(@field:NotBlank val content: String)
data class UpdateGuestbookRequest(@field:NotBlank val content: String)
