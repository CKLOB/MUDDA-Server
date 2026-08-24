package team.cklob.mudda.domain.block.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.block.domain.entity.Block

interface BlockRepository : JpaRepository<Block, Long> {
	fun existsByBlockerIdAndBlockedId(blockerId: Long, blockedId: Long): Boolean
	fun findByBlockerId(blockerId: Long): List<Block>

	// Bidirectional existence check: true if either member has blocked the other.
	fun existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(
		blockerId1: Long,
		blockedId1: Long,
		blockerId2: Long,
		blockedId2: Long,
	): Boolean

	@Query(
		"""
		SELECT CASE WHEN b.blocker.id = :memberId THEN b.blocked.id ELSE b.blocker.id END
		FROM Block b
		WHERE (b.blocker.id = :memberId AND b.blocked.id IN :otherIds)
		   OR (b.blocked.id = :memberId AND b.blocker.id IN :otherIds)
		""",
	)
	fun findBlockedMemberIds(@Param("memberId") memberId: Long, @Param("otherIds") otherIds: Collection<Long>): Set<Long>
}
