package team.cklob.mudda.domain.block.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.block.domain.entity.Block

interface BlockRepository : JpaRepository<Block, Long> {
	fun existsByBlockerIdAndBlockedId(blockerId: Long, blockedId: Long): Boolean
	fun findByBlockerId(blockerId: Long): List<Block>
	fun findByBlockerIdAndBlockedId(blockerId: Long, blockedId: Long): java.util.Optional<Block>

	// JOIN FETCH so rendering each row's nickname and profile image doesn't trigger an N+1 lazy load.
	@Query(
		value = "SELECT b FROM Block b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId ORDER BY b.createdAt DESC, b.id DESC",
		countQuery = "SELECT COUNT(b) FROM Block b WHERE b.blocker.id = :blockerId",
	)
	fun findByBlockerIdOrderByCreatedAtDesc(@Param("blockerId") blockerId: Long, pageable: Pageable): Page<Block>

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

	// Concurrent block requests would both pass a read-then-write check and collide on
	// uq_block_blocker_blocked, turning the loser into a 500. Inserting atomically lets the loser simply
	// observe 0 rows affected and read back the winner's row -- the same shape MediaRepository uses for
	// its own unique-key race. Doing this via an exception instead would poison the transaction and make
	// the follow-up read impossible.
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(
		value = """
			INSERT INTO tbl_block (blocker_id, blocked_id, created_at)
			VALUES (:blockerId, :blockedId, CURRENT_TIMESTAMP)
			ON CONFLICT (blocker_id, blocked_id) DO NOTHING
		""",
		nativeQuery = true,
	)
	fun insertIfAbsent(@Param("blockerId") blockerId: Long, @Param("blockedId") blockedId: Long): Int
}
