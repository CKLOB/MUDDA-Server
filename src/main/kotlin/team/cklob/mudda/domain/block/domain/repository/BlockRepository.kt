package team.cklob.mudda.domain.block.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.block.domain.entity.Block

interface BlockRepository : JpaRepository<Block, Long> {
	fun existsByBlockerIdAndBlockedId(blockerId: Long, blockedId: Long): Boolean
	fun findByBlockerId(blockerId: Long): List<Block>
}
