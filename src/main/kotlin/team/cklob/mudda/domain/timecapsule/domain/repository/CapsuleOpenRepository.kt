package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

	// Backs the discovery feed. Only PUBLIC, undeleted capsules surface -- a PRIVATE or FRIEND capsule
	// being opened is not public information. The member and capsule are fetched eagerly because every
	// row's response needs the nickname and capsule name, which would otherwise be one query each.
	// Ordering carries an id tie-breaker: openedAt alone leaves rows sharing a timestamp in a
	// database-chosen order, which can shift between requests and duplicate or skip an entry that
	// straddles a page boundary.
	@Query(
		"""
		SELECT o FROM CapsuleOpen o
		JOIN FETCH o.member
		JOIN FETCH o.timeCapsule c
		WHERE c.visibility = team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility.PUBLIC
			AND c.isDeleted = false
		ORDER BY o.openedAt DESC, o.id DESC
		""",
		countQuery = """
		SELECT COUNT(o) FROM CapsuleOpen o
		WHERE o.timeCapsule.visibility = team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility.PUBLIC
			AND o.timeCapsule.isDeleted = false
		""",
	)
	fun findPublicFeed(pageable: Pageable): Page<CapsuleOpen>
}
