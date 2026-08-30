package team.cklob.mudda.domain.block.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "회원 차단 요청")
data class CreateBlockRequest(
	@field:NotNull
	@Schema(description = "차단할 회원 ID", example = "2")
	val memberId: Long?,
)
