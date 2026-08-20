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
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.global.common.entity.BaseTimeEntity

@Entity
@Table(name = "tbl_guestbook")
class Guestbook(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_capsule_id", nullable = false)
	val timeCapsule: TimeCapsule,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	val member: Member,

	// No @Lob -- see the note on TimeCapsule.content: it would store a pg_largeobject OID here instead of
	// the text, and orphan the object when the row is deleted.
	@Column(nullable = false, columnDefinition = "TEXT")
	var content: String,

	@Column(name = "is_deleted", nullable = false)
	var isDeleted: Boolean = false,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseTimeEntity()
