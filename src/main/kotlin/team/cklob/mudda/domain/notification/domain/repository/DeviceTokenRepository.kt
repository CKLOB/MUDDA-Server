package team.cklob.mudda.domain.notification.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.notification.domain.entity.DeviceToken
import java.util.Optional

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {
	fun findByToken(token: String): Optional<DeviceToken>
	fun deleteByToken(token: String)

	@Query("SELECT d.token FROM DeviceToken d WHERE d.member.id = :memberId")
	fun findTokensByMemberId(@Param("memberId") memberId: Long): List<String>

	fun deleteByTokenIn(tokens: Collection<String>)
}
