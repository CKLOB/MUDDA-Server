package team.cklob.mudda.domain.friend.application.impl

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.domain.type.FriendRequestType
import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

class SearchFriendServiceTest {
	private val memberRepository = mockk<MemberRepository>()
	private val friendRepository = mockk<FriendRepository>()
	private val service = SearchFriendService(memberRepository, friendRepository)
	private val pageable = PageRequest.of(0, 20)

	private fun member(id: Long) = Member(
		name = "name-$id", nickname = "nickname-$id", email = "user$id@example.com",
		oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$id", profileVisibility = ProfileVisibility.PUBLIC, id = id,
	)

	@Test fun `returns NONE when there is no relationship with a candidate`() {
		val candidate = member(2L)
		every { memberRepository.searchSelectableByNickname(1L, "nick", pageable) } returns PageImpl(listOf(candidate), pageable, 1)
		every { friendRepository.findAllBetween(1L, listOf(2L)) } returns emptyList()

		val response = service.execute(1L, "nick", pageable)

		assertEquals(FriendStatus.NONE, response.content[0].relationStatus)
		assertNull(response.content[0].requestId)
		assertNull(response.content[0].requestDirection)
	}

	@Test fun `marks a candidate the caller already sent a request to`() {
		val candidate = member(2L)
		every { memberRepository.searchSelectableByNickname(1L, "nick", pageable) } returns PageImpl(listOf(candidate), pageable, 1)
		every { friendRepository.findAllBetween(1L, listOf(2L)) } returns
			listOf(Friend(requester = member(1L), receiver = candidate, status = FriendRequestStatus.PENDING, id = 10L))

		val response = service.execute(1L, "nick", pageable)

		assertEquals(FriendStatus.REQUESTED, response.content[0].relationStatus)
		assertEquals(10L, response.content[0].requestId)
		assertEquals(FriendRequestType.SENT, response.content[0].requestDirection)
	}

	@Test fun `marks a candidate who sent the caller a request`() {
		val candidate = member(2L)
		every { memberRepository.searchSelectableByNickname(1L, "nick", pageable) } returns PageImpl(listOf(candidate), pageable, 1)
		every { friendRepository.findAllBetween(1L, listOf(2L)) } returns
			listOf(Friend(requester = candidate, receiver = member(1L), status = FriendRequestStatus.PENDING, id = 10L))

		val response = service.execute(1L, "nick", pageable)

		assertEquals(FriendStatus.RECEIVED, response.content[0].relationStatus)
		assertEquals(FriendRequestType.RECEIVED, response.content[0].requestDirection)
	}

	@Test fun `marks an already-accepted friend`() {
		val candidate = member(2L)
		every { memberRepository.searchSelectableByNickname(1L, "nick", pageable) } returns PageImpl(listOf(candidate), pageable, 1)
		every { friendRepository.findAllBetween(1L, listOf(2L)) } returns
			listOf(Friend(requester = member(1L), receiver = candidate, status = FriendRequestStatus.ACCEPTED, id = 10L))

		val response = service.execute(1L, "nick", pageable)

		assertEquals(FriendStatus.FRIEND, response.content[0].relationStatus)
	}

	@Test fun `treats a rejected relationship as NONE`() {
		val candidate = member(2L)
		every { memberRepository.searchSelectableByNickname(1L, "nick", pageable) } returns PageImpl(listOf(candidate), pageable, 1)
		every { friendRepository.findAllBetween(1L, listOf(2L)) } returns
			listOf(Friend(requester = member(1L), receiver = candidate, status = FriendRequestStatus.REJECTED, id = 10L))

		val response = service.execute(1L, "nick", pageable)

		assertEquals(FriendStatus.NONE, response.content[0].relationStatus)
	}

	@Test fun `rejects a blank keyword`() {
		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, "   ", pageable) }
		assertEquals(ErrorCode.INVALID_SEARCH_KEYWORD, exception.errorCode)
	}

	@Test fun `trims the keyword before searching`() {
		every { memberRepository.searchSelectableByNickname(1L, "nick", pageable) } returns PageImpl(emptyList(), pageable, 0)

		service.execute(1L, "  nick  ", pageable)

		io.mockk.verify { memberRepository.searchSelectableByNickname(1L, "nick", pageable) }
		// LIKE-wildcard escaping is no longer this service's concern -- it's encapsulated in
		// MemberRepository#searchSelectableByNickname's 3-arg default method now, and verified for real
		// against Postgres by MemberRepositorySearchIntegrationTest's "escapes LIKE wildcard characters".
	}

	@Test fun `paginates results and reports page metadata`() {
		every { memberRepository.searchSelectableByNickname(1L, "nick", pageable) } returns PageImpl(emptyList(), pageable, 42)

		val response = service.execute(1L, "nick", pageable)

		assertEquals(42L, response.totalElements)
	}
}
