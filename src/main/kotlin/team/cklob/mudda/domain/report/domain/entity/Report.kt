package team.cklob.mudda.domain.report.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import team.cklob.mudda.domain.member.domain.entity.Member
import java.time.LocalDateTime

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

	@Column(name = "target_type", nullable = false, length = 30)
	val targetType: String,

	@Column(name = "target_id", nullable = false)
	val targetId: Long,

	@Column(nullable = false, length = 30)
	val reason: String,

	@Column(length = 500)
	val description: String? = null,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) {
	@Column(name = "created_at", nullable = false)
	var createdAt: LocalDateTime = LocalDateTime.now()
		protected set

	protected constructor() : this(Member("", "", "", null, null, ""), "", 0L, "", null)

	@PrePersist
	fun prePersist() {
		createdAt = LocalDateTime.now()
	}
}
