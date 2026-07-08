package team.cklob.mudda.domain.member.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import team.cklob.mudda.global.common.entity.BaseTimeEntity

@Entity
@Table(name = "tbl_member")
class Member(
	@Column(nullable = false, length = 30)
	var name: String,

	@Column(nullable = false, unique = true, length = 30)
	var nickname: String,

	@Column(nullable = false, unique = true, length = 255)
	val email: String,

	@Column(name = "profile_image_url", length = 255)
	var profileImageUrl: String? = null,

	@Column(length = 100)
	var bio: String? = null,

	@Column(name = "profile_visibility", nullable = false, length = 20)
	var profileVisibility: String,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseTimeEntity()
