package team.cklob.mudda.domain.notification.domain.entity

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
import team.cklob.mudda.global.common.entity.BaseCreatedAtEntity

// An FCM registration token for one installed app. A token is unique across the whole table, not per
// member: when a device is handed over to another account FCM reissues nothing, so the row is simply
// re-pointed at the new owner rather than duplicated.
@Entity
@Table(name = "tbl_device_token")
class DeviceToken(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	var member: Member,

	@Column(nullable = false, length = 255)
	val token: String,

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
) : BaseCreatedAtEntity()
