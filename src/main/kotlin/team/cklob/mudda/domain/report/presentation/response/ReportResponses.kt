package team.cklob.mudda.domain.report.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.report.domain.entity.Report
import team.cklob.mudda.domain.report.domain.type.ReportReason
import team.cklob.mudda.domain.report.domain.type.ReportTargetType
import java.time.LocalDateTime

@Schema(description = "신고 접수 응답")
data class CreateReportResponse(
	@Schema(description = "신고 ID", example = "1")
	val reportId: Long,

	@Schema(description = "신고 대상 종류", example = "CAPSULE")
	val targetType: ReportTargetType,

	@Schema(description = "신고 대상 ID", example = "12")
	val targetId: Long,

	@Schema(description = "신고 사유", example = "ABUSE")
	val reason: ReportReason,

	@Schema(description = "접수 시각")
	val createdAt: LocalDateTime,
) {
	companion object {
		fun from(report: Report) = CreateReportResponse(
			reportId = requireNotNull(report.id),
			targetType = report.targetType,
			targetId = report.targetId,
			reason = report.reason,
			createdAt = report.createdAt,
		)
	}
}
