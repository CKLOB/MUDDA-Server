package team.cklob.mudda.domain.timecapsule.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_time_capsule")
class TimeCapsule(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	val member: Member,

	@Column(nullable = false, length = 255)
	val name: String,

	@Lob
	@Column(columnDefinition = "TEXT")
	val content: String? = null,

	@Column(name = "time_capsule_type", nullable = false, length = 20)
	val timeCapsuleType: String,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	val visibility: CapsuleVisibility,

	@Enumerated(EnumType.STRING)
	@Column(name = "lock_type", nullable = false, length = 20)
	val lockType: CapsuleLockType,

	@Column(name = "password_hash", length = 255)
	val passwordHash: String? = null,

	@Column(length = 255)
	val question: String? = null,

	@Column(name = "answer_hash", length = 255)
	val answerHash: String? = null,

	@Column(nullable = false, columnDefinition = "geometry(Point,4326)")
	val location: Point,

	@Column(name = "location_name", length = 255)
	val locationName: String? = null,

	@Column(name = "open_radius_meter", nullable = false)
	val openRadiusMeter: Int,

	@Column(name = "expires_at", nullable = false)
	val expiresAt: LocalDateTime,

	@Column(name = "is_anonymous", nullable = false)
	val isAnonymous: Boolean,

	@Column(name = "is_feed_public", nullable = false)
	val isFeedPublic: Boolean,

	@Column(name = "is_deleted", nullable = false)
	val isDeleted: Boolean = false,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) {
	@Column(name = "created_at", nullable = false)
	var createdAt: LocalDateTime = LocalDateTime.now()
		protected set

	@Column(name = "updated_at", nullable = false)
	var updatedAt: LocalDateTime = LocalDateTime.now()
		protected set

	protected constructor() : this(
		Member("", "", "", null, null, ""),
		"",
		null,
		"",
		CapsuleVisibility.PRIVATE,
		CapsuleLockType.NONE,
		null,
		null,
		null,
		GeometryFactory().createPoint(Coordinate(0.0, 0.0)),
		null,
		0,
		LocalDateTime.now(),
		false,
		false,
		false,
	)

	@PrePersist
	fun prePersist() {
		val now = LocalDateTime.now()
		createdAt = now
		updatedAt = now
	}

	@PreUpdate
	fun preUpdate() {
		updatedAt = LocalDateTime.now()
	}
}
