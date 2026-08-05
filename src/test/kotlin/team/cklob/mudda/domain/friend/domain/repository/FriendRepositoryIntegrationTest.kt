package team.cklob.mudda.domain.friend.domain.repository

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import team.cklob.mudda.domain.block.domain.entity.Block
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.support.PostgresIntegrationTest
import java.time.LocalDateTime

// Exercises the real PostgreSQL/PostGIS schema produced by the actual Flyway migrations (including the
// V4 pending-pair/rejected-reuse unique indexes and the accepted_at CHECK constraint), which a MockK-based
// unit test cannot verify.
class FriendRepositoryIntegrationTest : PostgresIntegrationTest() {
	@Autowired private lateinit var friendRepository: FriendRepository
	@Autowired private lateinit var memberRepository: MemberRepository
	@Autowired private lateinit var blockRepository: BlockRepository
	@Autowired private lateinit var entityManager: EntityManager

	private fun member(tag: String) = memberRepository.saveAndFlush(
		Member(
			name = "name-$tag", nickname = "nickname-$tag", email = "user-$tag@example.com",
			oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$tag", profileVisibility = ProfileVisibility.PUBLIC,
		),
	)

	@Test fun `finds a relationship regardless of which side is the requester`() {
		val a = member("a")
		val b = member("b")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.PENDING))

		val fromAsFirst = friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(a.id!!, b.id!!, b.id!!, a.id!!)
		val fromBAsFirst = friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(b.id!!, a.id!!, a.id!!, b.id!!)

		assertEquals(1, fromAsFirst.size)
		assertEquals(1, fromBAsFirst.size)
	}

	@Test fun `findFriendships returns ACCEPTED relationships from either direction`() {
		val a = member("a")
		val b = member("b")
		val c = member("c")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.ACCEPTED, acceptedAt = LocalDateTime.now()))
		friendRepository.saveAndFlush(Friend(requester = c, receiver = a, status = FriendRequestStatus.ACCEPTED, acceptedAt = LocalDateTime.now()))
		friendRepository.saveAndFlush(Friend(requester = a, receiver = member("d"), status = FriendRequestStatus.PENDING))
		entityManager.flush()
		entityManager.clear()

		val page = friendRepository.findFriendships(a.id!!, PageRequest.of(0, 20))

		assertEquals(2, page.totalElements)
		assertTrue(page.content.all { it.status == FriendRequestStatus.ACCEPTED })
	}

	@Test fun `findFriendships excludes a friend blocked in either direction`() {
		val a = member("a")
		val kept = member("kept")
		val blockedByA = member("blocked-by-a")
		val blockedA = member("blocked-a")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = kept, status = FriendRequestStatus.ACCEPTED, acceptedAt = LocalDateTime.now()))
		friendRepository.saveAndFlush(Friend(requester = a, receiver = blockedByA, status = FriendRequestStatus.ACCEPTED, acceptedAt = LocalDateTime.now()))
		friendRepository.saveAndFlush(Friend(requester = blockedA, receiver = a, status = FriendRequestStatus.ACCEPTED, acceptedAt = LocalDateTime.now()))
		blockRepository.saveAndFlush(Block(blocker = a, blocked = blockedByA))
		blockRepository.saveAndFlush(Block(blocker = blockedA, blocked = a))
		entityManager.flush()
		entityManager.clear()

		val page = friendRepository.findFriendships(a.id!!, PageRequest.of(0, 20))

		val counterpartIds = page.content.map { if (it.requester.id == a.id) it.receiver.id else it.requester.id }
		assertEquals(listOf(kept.id), counterpartIds)
		// The block filter runs in SQL, not as a post-fetch step, so totalElements reflects it too.
		assertEquals(1L, page.totalElements)
	}

	@Test fun `same-direction duplicate PENDING request is rejected by the unique constraint`() {
		val a = member("a")
		val b = member("b")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.PENDING))

		assertThrows(DataIntegrityViolationException::class.java) {
			friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.PENDING))
		}
	}

	@Test fun `reverse-direction concurrent PENDING request is rejected by uq_friend_pending_pair`() {
		val a = member("a")
		val b = member("b")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.PENDING))

		assertThrows(DataIntegrityViolationException::class.java) {
			friendRepository.saveAndFlush(Friend(requester = b, receiver = a, status = FriendRequestStatus.PENDING))
		}
	}

	@Test fun `a rejected request can be sent again in the same direction`() {
		val a = member("a")
		val b = member("b")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.REJECTED))

		// Before V4's uq_friend_requester_receiver partial index (excluding REJECTED), this insert violated
		// the unconditional unique constraint from V2 and made re-requesting permanently impossible.
		val resent = friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.PENDING))

		assertEquals(FriendRequestStatus.PENDING, resent.status)
		assertEquals(2, friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(a.id!!, b.id!!, b.id!!, a.id!!).size)
	}

	@Test fun `an ACCEPTED row without accepted_at is rejected by ck_friend_accepted_at`() {
		val a = member("a")
		val b = member("b")

		assertThrows(DataIntegrityViolationException::class.java) {
			friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.ACCEPTED, acceptedAt = null))
		}
	}
}
