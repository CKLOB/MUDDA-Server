package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleRecipient
import java.util.Optional

interface CapsuleRecipientRepository : JpaRepository<CapsuleRecipient, Long> {
	fun findByMemberId(memberId: Long): List<CapsuleRecipient>
	@EntityGraph(attributePaths = ["timeCapsule", "timeCapsule.member"])
	fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<CapsuleRecipient>
	fun findAllByTimeCapsuleId(timeCapsuleId: Long): List<CapsuleRecipient>
	fun findByTimeCapsuleIdAndMemberId(timeCapsuleId: Long, memberId: Long): Optional<CapsuleRecipient>
	fun existsByTimeCapsuleIdAndMemberId(timeCapsuleId: Long, memberId: Long): Boolean

	@Query("SELECT r.timeCapsule.id FROM CapsuleRecipient r WHERE r.member.id = :memberId AND r.timeCapsule.id IN :capsuleIds")
	fun findReceivedCapsuleIds(@Param("memberId") memberId: Long, @Param("capsuleIds") capsuleIds: Collection<Long>): Set<Long>
}
