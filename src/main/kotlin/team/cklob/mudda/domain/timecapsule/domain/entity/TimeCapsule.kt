package team.cklob.mudda.domain.timecapsule.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.global.common.entity.BaseTimeEntity
import team.cklob.mudda.global.crypto.EncryptedStringConverter
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_time_capsule")
class TimeCapsule(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	val member: Member,

	@Column(nullable = false, length = 255)
	val name: String,

	// Encrypted at rest -- the column only ever holds a ContentCipher envelope blob, never the body itself.
	// No @Lob: on PostgreSQL that maps a String to a large object, so the column would hold an OID pointing
	// into pg_largeobject rather than the value itself, and the referenced object is not removed when the
	// row is deleted. `TEXT` is unbounded, so nothing is gained by the large-object indirection anyway.
	@Convert(converter = EncryptedStringConverter::class)
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
	var isDeleted: Boolean = false,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseTimeEntity()
