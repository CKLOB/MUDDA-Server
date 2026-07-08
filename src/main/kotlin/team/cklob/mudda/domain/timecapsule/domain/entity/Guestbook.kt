package team.cklob.mudda.domain.timecapsule.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.domain.timecapsule.domain.type.TimeCapsuleType
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_guestbook")
class Guestbook(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_capsule_id", nullable = false)
	val timeCapsule: TimeCapsule,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	val member: Member,

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	val content: String,

	@Column(name = "is_deleted", nullable = false)
	val isDeleted: Boolean = false,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) {
	@Column(name = "created_at", nullable = false)
	var createdAt: LocalDateTime = LocalDateTime.now()
		protected set

	protected constructor() : this(emptyTimeCapsule(), emptyMember(), "", false)

	@PrePersist
	fun prePersist() {
		createdAt = LocalDateTime.now()
	}

	companion object {
		private fun emptyMember() = Member("", "", "", null, null, ProfileVisibility.PRIVATE)

		private fun emptyTimeCapsule() = TimeCapsule(
			emptyMember(),
			"",
			null,
			TimeCapsuleType.NORMAL,
			CapsuleVisibility.PRIVATE,
			CapsuleLockType.NONE,
			null,
			null,
			null,
			org.locationtech.jts.geom.GeometryFactory().createPoint(org.locationtech.jts.geom.Coordinate(0.0, 0.0)),
			null,
			0,
			LocalDateTime.now(),
			false,
			false,
			false,
		)
	}
}
