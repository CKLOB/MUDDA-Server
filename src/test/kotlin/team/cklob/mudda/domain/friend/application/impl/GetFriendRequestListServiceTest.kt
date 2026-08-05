package team.cklob.mudda.domain.friend.application.impl

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.domain.type.FriendRequestType
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility

class GetFriendRequestListServiceTest {
	private val friendRepository = mockk<FriendRepository>()
	private val service = GetFriendRequestListService(friendRepository)
	private val pageable = PageRequest.of(0, 20)

	private fun member(id: Long) = Member(
		name = "name-$id", nickname = "nickname-$id", email = "user$id@example.com",
		oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$id", profileVisibility = ProfileVisibility.PUBLIC, id = id,
	)

	@Test fun `returns the requester as the counterpart for RECEIVED requests`() {
		val friend = Friend(requester = member(2L), receiver = member(1L), status = FriendRequestStatus.PENDING, id = 10L)
		every { friendRepository.findReceivedRequests(1L, FriendRequestStatus.PENDING, pageable) } returns PageImpl(listOf(friend), pageable, 1)

		val response = service.execute(1L, FriendRequestType.RECEIVED, FriendRequestStatus.PENDING, pageable)

		assertEquals(1, response.content.size)
		assertEquals(2L, response.content[0].memberId)
		assertEquals(FriendRequestType.RECEIVED, response.content[0].direction)
		assertEquals(10L, response.content[0].requestId)
	}

	@Test fun `returns the receiver as the counterpart for SENT requests`() {
		val friend = Friend(requester = member(1L), receiver = member(3L), status = FriendRequestStatus.PENDING, id = 11L)
		every { friendRepository.findSentRequests(1L, FriendRequestStatus.PENDING, pageable) } returns PageImpl(listOf(friend), pageable, 1)

		val response = service.execute(1L, FriendRequestType.SENT, FriendRequestStatus.PENDING, pageable)

		assertEquals(3L, response.content[0].memberId)
		assertEquals(FriendRequestType.SENT, response.content[0].direction)
	}

	@Test fun `applies the requested status filter`() {
		every { friendRepository.findReceivedRequests(1L, FriendRequestStatus.ACCEPTED, pageable) } returns PageImpl(emptyList(), pageable, 0)

		val response = service.execute(1L, FriendRequestType.RECEIVED, FriendRequestStatus.ACCEPTED, pageable)

		assertEquals(0, response.content.size)
	}

	@Test fun `maps page metadata from the repository result`() {
		// Page size 2 keeps offset(0) + pageSize(2) <= total(5), otherwise PageImpl silently recomputes
		// total down to offset + content.size() -- see https://github.com/spring-projects/spring-data-commons.
		val smallPage = PageRequest.of(0, 2)
		val friends = listOf(Friend(requester = member(2L), receiver = member(1L), status = FriendRequestStatus.PENDING, id = 10L), Friend(requester = member(3L), receiver = member(1L), status = FriendRequestStatus.PENDING, id = 11L))
		every { friendRepository.findReceivedRequests(1L, FriendRequestStatus.PENDING, smallPage) } returns PageImpl(friends, smallPage, 5)

		val response = service.execute(1L, FriendRequestType.RECEIVED, FriendRequestStatus.PENDING, smallPage)

		assertEquals(5L, response.totalElements)
		assertEquals(3, response.totalPages)
	}
}
