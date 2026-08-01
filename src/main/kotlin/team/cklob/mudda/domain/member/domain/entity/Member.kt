package team.cklob.mudda.domain.member.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.common.entity.BaseTimeEntity
import java.time.LocalDateTime

@Entity
@Table(name = "tbl_member")
class Member(
	@Column(length = 30)
	var name: String? = null,

	@Column(unique = true, length = 30)
	var nickname: String? = null,

	@Column(nullable = false, length = 255)
	var email: String,

	@Enumerated(EnumType.STRING)
	@Column(name = "oauth_provider", nullable = false, length = 20)
	val oauthProvider: OAuthProvider,

	// Mutable so a withdrawn row can be tombstoned (see LoginAuthService rejoin flow), freeing up
	// uq_member_oauth_provider_provider_id for a fresh signup with the same real provider id.
	@Column(name = "provider_id", nullable = false, length = 255)
	var providerId: String,

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	var gender: Gender? = null,

	@Column(name = "birth_year")
	var birthYear: Int? = null,

	@Column(name = "profile_image_url", length = 255)
	var profileImageUrl: String? = null,

	@Column(length = 100)
	var bio: String? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "profile_visibility", nullable = false, length = 20)
	var profileVisibility: ProfileVisibility,

	@Column(name = "withdrawn_at")
	var withdrawnAt: LocalDateTime? = null,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseTimeEntity()
