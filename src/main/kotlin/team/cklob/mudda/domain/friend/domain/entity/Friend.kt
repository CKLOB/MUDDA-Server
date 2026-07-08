package team.cklob.mudda.domain.friend.domain.entity

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
import jakarta.persistence.UniqueConstraint
import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import java.time.LocalDateTime

@Entity
@Table(
	name = "tbl_friend",
	uniqueConstraints = [
		UniqueConstraint(
			name = "uq_friend_requester_receiver",
			columnNames = ["requester_id", "receiver_id"],
		),
	],
)
class Friend(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "requester_id", nullable = false)
	val requester: Member,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver_id", nullable = false)
	val receiver: Member,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	val status: FriendStatus,

	@Column(name = "accepted_at")
	val acceptedAt: LocalDateTime? = null,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) {
	@Column(name = "created_at", nullable = false)
	var createdAt: LocalDateTime = LocalDateTime.now()
		protected set

	protected constructor() : this(emptyMember(), emptyMember(), FriendStatus.PENDING, null)

	@PrePersist
	fun prePersist() {
		createdAt = LocalDateTime.now()
	}

	companion object {
		private fun emptyMember() = Member("", "", "", null, null, ProfileVisibility.PRIVATE)
	}
}
