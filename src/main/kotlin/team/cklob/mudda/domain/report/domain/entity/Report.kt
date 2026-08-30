package team.cklob.mudda.domain.report.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.report.domain.type.ReportReason
import team.cklob.mudda.domain.report.domain.type.ReportTargetType
import team.cklob.mudda.global.common.entity.BaseCreatedAtEntity

@Entity
@Table(
	name = "tbl_report",
	uniqueConstraints = [
		UniqueConstraint(
			name = "uq_report_reporter_target",
			columnNames = ["reporter_id", "target_type", "target_id"],
		),
	],
)
class Report(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	val reporter: Member,

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 30)
	val targetType: ReportTargetType,

	@Column(name = "target_id", nullable = false)
	val targetId: Long,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	val reason: ReportReason,

	@Column(length = 500)
	val description: String? = null,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseCreatedAtEntity()
