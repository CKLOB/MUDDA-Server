package team.cklob.mudda.domain.friend.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.friend.domain.entity.Friend
import java.util.Optional

interface FriendRepository : JpaRepository<Friend, Long> {
	fun findByRequesterIdOrReceiverId(requesterId: Long, receiverId: Long): List<Friend>
	fun findByRequesterIdAndReceiverId(requesterId: Long, receiverId: Long): Optional<Friend>
	fun existsByRequesterIdAndReceiverId(requesterId: Long, receiverId: Long): Boolean
}
