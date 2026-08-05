package team.cklob.mudda.domain.friend.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

class DeleteFriendServiceTest {
	private val friendRepository = mockk<FriendRepository>()
	private val service = DeleteFriendService(friendRepository)

	private fun member(id: Long) = Member(
		name = "name-$id", nickname = "nickname-$id", email = "user$id@example.com",
		oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$id", profileVisibility = ProfileVisibility.PUBLIC, id = id,
	)

	@Test fun `deletes the friendship when the caller was the requester`() {
		val friend = Friend(requester = member(1L), receiver = member(2L), status = FriendRequestStatus.ACCEPTED, id = 10L)
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns listOf(friend)
		every { friendRepository.delete(friend) } returns Unit

		service.execute(1L, 2L)

		verify(exactly = 1) { friendRepository.delete(friend) }
	}

	@Test fun `deletes the friendship when the caller was the receiver`() {
		val friend = Friend(requester = member(2L), receiver = member(1L), status = FriendRequestStatus.ACCEPTED, id = 10L)
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns listOf(friend)
		every { friendRepository.delete(friend) } returns Unit

		service.execute(1L, 2L)

		verify(exactly = 1) { friendRepository.delete(friend) }
	}

	@Test fun `rejects when there is no relationship at all`() {
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns emptyList()

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, 2L) }
		assertEquals(ErrorCode.FRIEND_NOT_FOUND, exception.errorCode)
	}

	@Test fun `rejects deleting a pending (not yet accepted) relationship`() {
		val friend = Friend(requester = member(1L), receiver = member(2L), status = FriendRequestStatus.PENDING, id = 10L)
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns listOf(friend)

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, 2L) }
		assertEquals(ErrorCode.FRIEND_NOT_FOUND, exception.errorCode)
		verify(exactly = 0) { friendRepository.delete(any()) }
	}
}
