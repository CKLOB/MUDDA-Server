package team.cklob.mudda.domain.member.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_member")
class Member(
	@Column(nullable = false, length = 30)
	val name: String,

	@Column(nullable = false, unique = true, length = 30)
	val nickname: String,

	@Column(nullable = false, unique = true, length = 255)
	val email: String,

	@Column(name = "profile_image_url", length = 255)
	val profileImageUrl: String? = null,

	@Column(length = 100)
	val bio: String? = null,

	@Column(name = "profile_visibility", nullable = false, length = 20)
	val profileVisibility: String,

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

	protected constructor() : this("", "", "", null, null, "")

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
