package team.cklob.mudda.domain.block.domain.entity

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
	name = "tbl_block",
	uniqueConstraints = [
		UniqueConstraint(
			name = "uq_block_blocker_blocked",
			columnNames = ["blocker_id", "blocked_id"],
		),
	],
)
class Block(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "blocker_id", nullable = false)
	val blocker: Member,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "blocked_id", nullable = false)
	val blocked: Member,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) {
	@Column(name = "created_at", nullable = false)
	var createdAt: LocalDateTime = LocalDateTime.now()
		protected set

	protected constructor() : this(emptyMember(), emptyMember())

	@PrePersist
	fun prePersist() {
		createdAt = LocalDateTime.now()
	}

	companion object {
		private fun emptyMember() = Member("", "", "", null, null, "")
	}
}
