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
import team.cklob.mudda.global.common.entity.BaseCreatedAtEntity

// One Shamir share of a CLIENT_E2E capsule's content key.
//
// The server deliberately holds fewer usable shares than the threshold. A wrapped share is stored as
// ciphertext under a key derived from the capsule's password or answer, which the server only knows as a
// bcrypt hash -- so it counts toward the client's quorum but never toward the server's.
@Entity
@Table(
	name = "tbl_key_share",
	uniqueConstraints = [
		UniqueConstraint(name = "uq_key_share_capsule_index", columnNames = ["time_capsule_id", "share_index"]),
	],
)
class KeyShare(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_capsule_id", nullable = false)
	val timeCapsule: TimeCapsule,

	// The Shamir x-coordinate. Interpolation cannot recover the key without it, so it travels with the
	// share rather than being implied by row order.
	@Column(name = "share_index", nullable = false)
	val shareIndex: Int,

	@Column(name = "share_data", nullable = false, columnDefinition = "TEXT")
	val shareData: String,

	@Column(name = "is_wrapped", nullable = false)
	val isWrapped: Boolean,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseCreatedAtEntity()
