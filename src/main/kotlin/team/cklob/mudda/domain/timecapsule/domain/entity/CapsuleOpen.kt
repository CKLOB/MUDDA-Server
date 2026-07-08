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
import org.locationtech.jts.geom.Point
import team.cklob.mudda.domain.member.domain.entity.Member
import java.time.LocalDateTime

@Entity
@Table(
	name = "tbl_capsule_open",
	uniqueConstraints = [
		UniqueConstraint(
			name = "uq_capsule_open_time_capsule_member",
			columnNames = ["time_capsule_id", "member_id"],
		),
	],
)
class CapsuleOpen(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_capsule_id", nullable = false)
	val timeCapsule: TimeCapsule,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	val member: Member,

	@Column(name = "opened_at", nullable = false)
	val openedAt: LocalDateTime,

	@Column(name = "open_location", columnDefinition = "geometry(Point,4326)")
	val openLocation: Point? = null,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
)
