package team.cklob.mudda.domain.friend.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
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

	// Fetches every relationship row (any status) between the viewer and a batch of other member ids in a
	// single query, so a search-result page or similar batch lookup doesn't issue one query per candidate.
	@Query(
		"""
		SELECT f FROM Friend f
		WHERE (f.requester.id = :memberId AND f.receiver.id IN :otherIds)
		   OR (f.receiver.id = :memberId AND f.requester.id IN :otherIds)
		""",
	)
	fun findAllBetween(@Param("memberId") memberId: Long, @Param("otherIds") otherIds: Collection<Long>): List<Friend>

	// requester/receiver are eagerly fetched so the response mapping (counterpart nickname/profileImageUrl)
	// doesn't trigger an N+1 lazy load per row.
	@Query(
		"""
		SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver
		WHERE f.status = :status AND (f.requester.id = :memberId OR f.receiver.id = :memberId)
		ORDER BY f.acceptedAt DESC
		""",
	)
	fun findFriendships(@Param("memberId") memberId: Long, @Param("status") status: FriendRequestStatus, pageable: Pageable): Page<Friend>

	@Query(
		"""
		SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver
		WHERE f.receiver.id = :receiverId AND f.status = :status
		ORDER BY f.createdAt DESC
		""",
	)
	fun findReceivedRequests(@Param("receiverId") receiverId: Long, @Param("status") status: FriendRequestStatus, pageable: Pageable): Page<Friend>

	@Query(
		"""
		SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver
		WHERE f.requester.id = :requesterId AND f.status = :status
		ORDER BY f.createdAt DESC
		""",
	)
	fun findSentRequests(@Param("requesterId") requesterId: Long, @Param("status") status: FriendRequestStatus, pageable: Pageable): Page<Friend>
}
