package team.cklob.mudda.domain.report.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import team.cklob.mudda.domain.report.domain.type.ReportReason
import team.cklob.mudda.domain.report.domain.type.ReportTargetType

@Schema(description = "신고 요청")
data class CreateReportRequest(
	@field:NotNull
	@Schema(description = "신고 대상 종류", example = "CAPSULE")
	val targetType: ReportTargetType?,

	@field:NotNull
	@Schema(description = "신고 대상 ID", example = "12")
	val targetId: Long?,

	@field:NotNull
	@Schema(description = "신고 사유", example = "ABUSE")
	val reason: ReportReason?,

	@field:Size(max = 500)
	@Schema(description = "상세 사유. reason이 ETC일 때는 필수입니다.", example = "욕설이 포함되어 있습니다.", nullable = true)
	val description: String? = null,
)
