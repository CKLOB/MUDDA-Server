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
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleEncryptionMode
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

	// Never holds the body in plaintext. For SERVER_ENVELOPE capsules it is a ContentCipher envelope the
	// server can open; for CLIENT_E2E capsules it is a blob the client encrypted under a key the server
	// never received, and the converter simply adds a second at-rest layer over ciphertext.
	//
	// No @Lob: on PostgreSQL that maps a String to a large object, so the column would hold an OID pointing
	// into pg_largeobject rather than the value itself, and the referenced object is not removed when the
	// row is deleted. `TEXT` is unbounded, so nothing is gained by the large-object indirection anyway.
	@Convert(converter = EncryptedStringConverter::class)
	@Column(columnDefinition = "TEXT")
	val content: String? = null,

	// Which side holds the key. Read the mode rather than inferring it from lockType: the open path must
	// never hand back a body for a capsule the server is not supposed to be able to read.
	@Enumerated(EnumType.STRING)
	@Column(name = "encryption_mode", nullable = false, length = 20)
	val encryptionMode: CapsuleEncryptionMode = CapsuleEncryptionMode.SERVER_ENVELOPE,

	// Number of shares needed to rebuild the content key. Null for SERVER_ENVELOPE capsules.
	@Column(name = "key_threshold")
	val keyThreshold: Int? = null,

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

	@Column(name = "open_at", nullable = false)
	val openAt: LocalDateTime,

	@Column(name = "expires_at")
	val expiredAt: LocalDateTime? = null,

	@Column(name = "is_deleted", nullable = false)
	var isDeleted: Boolean = false,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseTimeEntity()
