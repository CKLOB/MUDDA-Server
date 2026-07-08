package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility

interface TimeCapsuleRepository : JpaRepository<TimeCapsule, Long> {
	fun findByMemberIdAndIsDeletedFalse(memberId: Long): List<TimeCapsule>
	fun findByVisibilityAndIsDeletedFalse(visibility: CapsuleVisibility): List<TimeCapsule>
}
