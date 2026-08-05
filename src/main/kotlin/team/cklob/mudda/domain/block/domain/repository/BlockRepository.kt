package team.cklob.mudda.domain.block.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
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

	// All block rows where the given member is on either side, used to build a single member's full
	// bidirectional block set in one query (e.g. filtering the friend list) instead of per-row lookups.
	fun findByBlockerIdOrBlockedId(blockerId: Long, blockedId: Long): List<Block>
}
