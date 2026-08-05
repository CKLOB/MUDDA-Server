package team.cklob.mudda.domain.friend.application.impl

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import team.cklob.mudda.domain.block.domain.entity.Block
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import java.time.LocalDateTime

class GetFriendListServiceTest {
	private val friendRepository = mockk<FriendRepository>()
	private val blockRepository = mockk<BlockRepository>()
	private val service = GetFriendListService(friendRepository, blockRepository)
	private val pageable = PageRequest.of(0, 20)

	private fun member(id: Long) = Member(
		name = "name-$id", nickname = "nickname-$id", email = "user$id@example.com",
		oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$id", profileVisibility = ProfileVisibility.PUBLIC, id = id,
	)

	private fun mockNoBlocks() {
		every { blockRepository.findByBlockerIdOrBlockedId(1L, 1L) } returns emptyList()
	}

	@Test fun `returns the counterpart when the caller was the requester`() {
		val acceptedAt = LocalDateTime.now()
		val friend = Friend(requester = member(1L), receiver = member(2L), status = FriendRequestStatus.ACCEPTED, acceptedAt = acceptedAt, id = 10L)
		every { friendRepository.findFriendships(1L, FriendRequestStatus.ACCEPTED, pageable) } returns PageImpl(listOf(friend), pageable, 1)
		mockNoBlocks()

		val response = service.execute(1L, pageable)

		assertEquals(1, response.content.size)
		assertEquals(2L, response.content[0].memberId)
		assertEquals(acceptedAt, response.content[0].acceptedAt)
	}

	@Test fun `returns the counterpart when the caller was the receiver`() {
		val acceptedAt = LocalDateTime.now()
		val friend = Friend(requester = member(2L), receiver = member(1L), status = FriendRequestStatus.ACCEPTED, acceptedAt = acceptedAt, id = 10L)
		every { friendRepository.findFriendships(1L, FriendRequestStatus.ACCEPTED, pageable) } returns PageImpl(listOf(friend), pageable, 1)
		mockNoBlocks()

		val response = service.execute(1L, pageable)

		assertEquals(2L, response.content[0].memberId)
	}

	@Test fun `only queries ACCEPTED relationships`() {
		mockNoBlocks()
		every { friendRepository.findFriendships(1L, FriendRequestStatus.ACCEPTED, pageable) } returns PageImpl(emptyList(), pageable, 0)

		val response = service.execute(1L, pageable)

		assertTrue(response.content.isEmpty())
	}

	@Test fun `filters out a friend that is in a block relationship with the caller`() {
		val acceptedAt = LocalDateTime.now()
		val kept = Friend(requester = member(1L), receiver = member(2L), status = FriendRequestStatus.ACCEPTED, acceptedAt = acceptedAt, id = 10L)
		val blocked = Friend(requester = member(1L), receiver = member(3L), status = FriendRequestStatus.ACCEPTED, acceptedAt = acceptedAt, id = 11L)
		every { friendRepository.findFriendships(1L, FriendRequestStatus.ACCEPTED, pageable) } returns PageImpl(listOf(kept, blocked), pageable, 2)
		every { blockRepository.findByBlockerIdOrBlockedId(1L, 1L) } returns listOf(Block(blocker = member(1L), blocked = member(3L), id = 100L))

		val response = service.execute(1L, pageable)

		assertEquals(1, response.content.size)
		assertEquals(2L, response.content[0].memberId)
	}

	@Test fun `maps page metadata and sorting order from the repository result`() {
		val older = Friend(requester = member(1L), receiver = member(2L), status = FriendRequestStatus.ACCEPTED, acceptedAt = LocalDateTime.now().minusDays(1), id = 10L)
		val newer = Friend(requester = member(1L), receiver = member(3L), status = FriendRequestStatus.ACCEPTED, acceptedAt = LocalDateTime.now(), id = 11L)
		// The repository query itself orders by acceptedAt DESC; the service must preserve that order, not re-sort.
		every { friendRepository.findFriendships(1L, FriendRequestStatus.ACCEPTED, pageable) } returns PageImpl(listOf(newer, older), pageable, 2)
		mockNoBlocks()

		val response = service.execute(1L, pageable)

		assertEquals(listOf(3L, 2L), response.content.map { it.memberId })
		assertEquals(2L, response.totalElements)
		assertEquals(1, response.totalPages)
	}
}
