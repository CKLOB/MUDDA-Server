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

	@Query(
		"""
		SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friend f
		WHERE f.status = team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus.ACCEPTED
		  AND ((f.requester.id = :firstId AND f.receiver.id = :secondId)
		    OR (f.requester.id = :secondId AND f.receiver.id = :firstId))
		""",
	)
	fun existsAcceptedBetween(@Param("firstId") firstId: Long, @Param("secondId") secondId: Long): Boolean

	@Query(
		"""
		SELECT CASE WHEN f.requester.id = :memberId THEN f.receiver.id ELSE f.requester.id END
		FROM Friend f
		WHERE f.status = team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus.ACCEPTED
		  AND ((f.requester.id = :memberId AND f.receiver.id IN :otherIds)
		    OR (f.receiver.id = :memberId AND f.requester.id IN :otherIds))
		""",
	)
	fun findAcceptedCounterpartIds(@Param("memberId") memberId: Long, @Param("otherIds") otherIds: Collection<Long>): Set<Long>

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
	// doesn't trigger an N+1 lazy load per row. status is hardcoded to ACCEPTED (the only caller,
	// GetFriendListService, always wants that; ORDER BY acceptedAt is meaningless for any other status
	// since the column is only ever populated for ACCEPTED rows anyway). Blocked counterparts are excluded
	// in SQL -- the same NOT EXISTS shape as MemberRepository#searchSelectableByNickname -- so pagination
	// metadata (totalElements/totalPages/hasNext) stays accurate instead of drifting from a post-fetch
	// filter. f.id DESC breaks ties for rows that share the same acceptedAt second, which is common when
	// requests are accepted in a batch, so a stable page boundary doesn't skip or repeat a row.
	@Query(
		value = """
			SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver
			WHERE f.status = team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus.ACCEPTED
			  AND (f.requester.id = :memberId OR f.receiver.id = :memberId)
			  AND NOT EXISTS (
				  SELECT 1 FROM Block b
				  WHERE (b.blocker.id = :memberId AND b.blocked.id = CASE WHEN f.requester.id = :memberId THEN f.receiver.id ELSE f.requester.id END)
				     OR (b.blocked.id = :memberId AND b.blocker.id = CASE WHEN f.requester.id = :memberId THEN f.receiver.id ELSE f.requester.id END)
			  )
			ORDER BY f.acceptedAt DESC, f.id DESC
		""",
		countQuery = """
			SELECT COUNT(f) FROM Friend f
			WHERE f.status = team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus.ACCEPTED
			  AND (f.requester.id = :memberId OR f.receiver.id = :memberId)
			  AND NOT EXISTS (
				  SELECT 1 FROM Block b
				  WHERE (b.blocker.id = :memberId AND b.blocked.id = CASE WHEN f.requester.id = :memberId THEN f.receiver.id ELSE f.requester.id END)
				     OR (b.blocked.id = :memberId AND b.blocker.id = CASE WHEN f.requester.id = :memberId THEN f.receiver.id ELSE f.requester.id END)
			  )
		""",
	)
	fun findFriendships(@Param("memberId") memberId: Long, pageable: Pageable): Page<Friend>

	// Blocked counterparts are excluded in SQL, the same NOT EXISTS shape findFriendships uses. Without it
	// a blocked member's pending request stays visible even though RespondFriendRequestService re-checks
	// the block and refuses the accept -- the receiver would see a request they can never act on.
	@Query(
		value = """
		SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver
		WHERE f.receiver.id = :receiverId AND f.status = :status
		  AND NOT EXISTS (
			  SELECT 1 FROM Block b
			  WHERE (b.blocker.id = :receiverId AND b.blocked.id = f.requester.id)
			     OR (b.blocked.id = :receiverId AND b.blocker.id = f.requester.id)
		  )
		ORDER BY f.createdAt DESC, f.id DESC
		""",
		countQuery = """
		SELECT COUNT(f) FROM Friend f
		WHERE f.receiver.id = :receiverId AND f.status = :status
		  AND NOT EXISTS (
			  SELECT 1 FROM Block b
			  WHERE (b.blocker.id = :receiverId AND b.blocked.id = f.requester.id)
			     OR (b.blocked.id = :receiverId AND b.blocker.id = f.requester.id)
		  )
		""",
	)
	fun findReceivedRequests(@Param("receiverId") receiverId: Long, @Param("status") status: FriendRequestStatus, pageable: Pageable): Page<Friend>

	@Query(
		value = """
		SELECT f FROM Friend f JOIN FETCH f.requester JOIN FETCH f.receiver
		WHERE f.requester.id = :requesterId AND f.status = :status
		  AND NOT EXISTS (
			  SELECT 1 FROM Block b
			  WHERE (b.blocker.id = :requesterId AND b.blocked.id = f.receiver.id)
			     OR (b.blocked.id = :requesterId AND b.blocker.id = f.receiver.id)
		  )
		ORDER BY f.createdAt DESC, f.id DESC
		""",
		countQuery = """
		SELECT COUNT(f) FROM Friend f
		WHERE f.requester.id = :requesterId AND f.status = :status
		  AND NOT EXISTS (
			  SELECT 1 FROM Block b
			  WHERE (b.blocker.id = :requesterId AND b.blocked.id = f.receiver.id)
			     OR (b.blocked.id = :requesterId AND b.blocker.id = f.receiver.id)
		  )
		""",
	)
	fun findSentRequests(@Param("requesterId") requesterId: Long, @Param("status") status: FriendRequestStatus, pageable: Pageable): Page<Friend>
}
