package team.cklob.mudda.domain.media.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import team.cklob.mudda.domain.media.domain.type.MediaType
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.domain.timecapsule.domain.type.TimeCapsuleType
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_media")
class Media(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_capsule_id", nullable = false)
	val timeCapsule: TimeCapsule,

	@Enumerated(EnumType.STRING)
	@Column(name = "media_type", nullable = false, length = 20)
	val mediaType: MediaType,

	@Column(name = "media_url", nullable = false, length = 255)
	val mediaUrl: String,

	@Column(name = "s3_key", nullable = false, length = 255)
	val s3Key: String,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) {
	@Column(name = "created_at", nullable = false)
	var createdAt: LocalDateTime = LocalDateTime.now()
		protected set

	protected constructor() : this(emptyTimeCapsule(), MediaType.IMAGE, "", "")

	@PrePersist
	fun prePersist() {
		createdAt = LocalDateTime.now()
	}

	companion object {
		private fun emptyTimeCapsule() = TimeCapsule(
			Member("", "", "", null, null, ProfileVisibility.PRIVATE),
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
