package team.cklob.mudda.domain.friend.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.friend.domain.entity.Friend
import java.util.Optional

interface FriendRepository : JpaRepository<Friend, Long> {
	fun findByRequesterIdOrReceiverId(requesterId: Long, receiverId: Long): List<Friend>
	fun findByRequesterIdAndReceiverId(requesterId: Long, receiverId: Long): Optional<Friend>
	fun existsByRequesterIdAndReceiverId(requesterId: Long, receiverId: Long): Boolean

	// uq_friend_requester_receiver only blocks a duplicate row in the same direction, so a requester/receiver
	// pair can still have two rows (e.g. both sides sent a request before either was accepted). Returning a
	// List keeps that a normal case instead of an Optional throwing IncorrectResultSizeDataAccessException.
	fun findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(
		requesterId1: Long,
		receiverId1: Long,
		requesterId2: Long,
		receiverId2: Long,
	): List<Friend>
}
