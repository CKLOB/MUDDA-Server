package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleOpen
import java.util.Optional

interface CapsuleOpenRepository : JpaRepository<CapsuleOpen, Long> {
	fun findByTimeCapsuleIdAndMemberId(timeCapsuleId: Long, memberId: Long): Optional<CapsuleOpen>
	fun existsByTimeCapsuleIdAndMemberId(timeCapsuleId: Long, memberId: Long): Boolean
}
