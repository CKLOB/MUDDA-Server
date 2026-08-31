package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.timecapsule.domain.entity.KeyShare
import java.time.LocalDateTime

interface KeyShareRepository : JpaRepository<KeyShare, Long> {
	fun findAllByTimeCapsuleIdOrderByShareIndex(timeCapsuleId: Long): List<KeyShare>

	// An expired capsule can never be opened again, so its shares are dead weight that would only widen
	// the window for an offline attack on a wrapped share. The encryption design calls for removing them
	// on a schedule.
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(
		"""
		DELETE FROM KeyShare k WHERE k.timeCapsule.id IN (
			SELECT c.id FROM TimeCapsule c WHERE c.expiredAt IS NOT NULL AND c.expiredAt <= :now
		)
		""",
	)
	fun deleteSharesOfCapsulesExpiredBefore(@Param("now") now: LocalDateTime): Int
}
