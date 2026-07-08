package team.cklob.mudda.domain.timecapsule.domain.entity

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
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.global.common.entity.BaseCreatedAtEntity
import java.time.LocalDateTime

@Entity
@Table(
	name = "tbl_capsule_recipient",
	uniqueConstraints = [
		UniqueConstraint(
			name = "uq_capsule_recipient_time_capsule_member",
			columnNames = ["time_capsule_id", "member_id"],
		),
	],
)
class CapsuleRecipient(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	val member: Member,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_capsule_id", nullable = false)
	val timeCapsule: TimeCapsule,

	@Column(name = "has_opened", nullable = false)
	var hasOpened: Boolean = false,

	@Column(name = "opened_at")
	var openedAt: LocalDateTime? = null,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseCreatedAtEntity()
