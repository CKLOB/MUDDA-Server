package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleRecipient
import java.util.Optional

interface CapsuleRecipientRepository : JpaRepository<CapsuleRecipient, Long> {
	fun findByMemberId(memberId: Long): List<CapsuleRecipient>
	fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<CapsuleRecipient>
	fun findAllByTimeCapsuleId(timeCapsuleId: Long): List<CapsuleRecipient>
	fun findByTimeCapsuleIdAndMemberId(timeCapsuleId: Long, memberId: Long): Optional<CapsuleRecipient>
	fun existsByTimeCapsuleIdAndMemberId(timeCapsuleId: Long, memberId: Long): Boolean
}
