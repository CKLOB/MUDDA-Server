package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleOpen
import java.util.Optional

interface CapsuleOpenRepository : JpaRepository<CapsuleOpen, Long> {
	fun findByTimeCapsuleIdAndMemberId(timeCapsuleId: Long, memberId: Long): Optional<CapsuleOpen>
	fun existsByTimeCapsuleIdAndMemberId(timeCapsuleId: Long, memberId: Long): Boolean

	@Query("SELECT o.timeCapsule.id FROM CapsuleOpen o WHERE o.member.id = :memberId AND o.timeCapsule.id IN :capsuleIds")
	fun findOpenedCapsuleIds(@Param("memberId") memberId: Long, @Param("capsuleIds") capsuleIds: Collection<Long>): Set<Long>
}
