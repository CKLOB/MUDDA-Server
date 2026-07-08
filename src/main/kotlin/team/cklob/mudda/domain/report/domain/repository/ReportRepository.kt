package team.cklob.mudda.domain.report.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.report.domain.entity.Report
import team.cklob.mudda.domain.report.domain.type.ReportTargetType

interface ReportRepository : JpaRepository<Report, Long> {
	fun existsByReporterIdAndTargetTypeAndTargetId(
		reporterId: Long,
		targetType: ReportTargetType,
		targetId: Long,
	): Boolean
}
