package team.cklob.mudda.domain.friend.application.impl

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.notification.application.impl.NotificationPublisher
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.domain.friend.domain.type.FriendRequestAction
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.presentation.request.RespondFriendRequestRequest
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.util.Optional

class RespondFriendRequestServiceTest {
	private val friendRepository = mockk<FriendRepository>()
	private val blockRepository = mockk<BlockRepository>()
	private val notificationPublisher = mockk<NotificationPublisher>(relaxed = true)
	private val service = RespondFriendRequestService(friendRepository, blockRepository, notificationPublisher)

	private fun member(id: Long) = Member(
		name = "name-$id", nickname = "nickname-$id", email = "user$id@example.com",
		oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$id", profileVisibility = ProfileVisibility.PUBLIC, id = id,
	)

	private fun pendingRequest(requesterId: Long = 1L, receiverId: Long = 2L) =
		Friend(requester = member(requesterId), receiver = member(receiverId), status = FriendRequestStatus.PENDING, id = 10L)

	private fun mockNoBlock(receiverId: Long = 2L, requesterId: Long = 1L) {
		every { blockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(receiverId, requesterId, requesterId, receiverId) } returns false
	}

	@Test fun `accepts a pending request addressed to the caller`() {
		val friend = pendingRequest()
		every { friendRepository.findById(10L) } returns Optional.of(friend)
		mockNoBlock()

		service.execute(2L, 10L, RespondFriendRequestRequest(FriendRequestAction.ACCEPT))

		assertEquals(FriendRequestStatus.ACCEPTED, friend.status)
		assertNotNull(friend.acceptedAt)
	}

	@Test fun `rejects a pending request addressed to the caller`() {
		val friend = pendingRequest()
		every { friendRepository.findById(10L) } returns Optional.of(friend)

		service.execute(2L, 10L, RespondFriendRequestRequest(FriendRequestAction.REJECT))

		assertEquals(FriendRequestStatus.REJECTED, friend.status)
		assertNull(friend.acceptedAt)
	}

	@Test fun `rejects when the caller is not the receiver`() {
		val friend = pendingRequest()
		every { friendRepository.findById(10L) } returns Optional.of(friend)

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, 10L, RespondFriendRequestRequest(FriendRequestAction.ACCEPT)) }
		assertEquals(ErrorCode.FRIEND_REQUEST_NOT_RECEIVER, exception.errorCode)
		assertEquals(FriendRequestStatus.PENDING, friend.status)
	}

	@Test fun `rejects when the request does not exist`() {
		every { friendRepository.findById(99L) } returns Optional.empty()

		val exception = assertThrows(BusinessException::class.java) { service.execute(2L, 99L, RespondFriendRequestRequest(FriendRequestAction.ACCEPT)) }
		assertEquals(ErrorCode.FRIEND_REQUEST_NOT_FOUND, exception.errorCode)
	}

	@Test fun `rejects responding to an already-accepted request`() {
		val friend = pendingRequest().apply { status = FriendRequestStatus.ACCEPTED }
		every { friendRepository.findById(10L) } returns Optional.of(friend)

		val exception = assertThrows(BusinessException::class.java) { service.execute(2L, 10L, RespondFriendRequestRequest(FriendRequestAction.ACCEPT)) }
		assertEquals(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED, exception.errorCode)
	}

	@Test fun `rejects responding to an already-rejected request`() {
		val friend = pendingRequest().apply { status = FriendRequestStatus.REJECTED }
		every { friendRepository.findById(10L) } returns Optional.of(friend)

		val exception = assertThrows(BusinessException::class.java) { service.execute(2L, 10L, RespondFriendRequestRequest(FriendRequestAction.REJECT)) }
		assertEquals(ErrorCode.FRIEND_REQUEST_ALREADY_PROCESSED, exception.errorCode)
	}

	@Test fun `rejects accepting when a block exists between the two members`() {
		val friend = pendingRequest()
		every { friendRepository.findById(10L) } returns Optional.of(friend)
		every { blockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(2L, 1L, 1L, 2L) } returns true

		val exception = assertThrows(BusinessException::class.java) { service.execute(2L, 10L, RespondFriendRequestRequest(FriendRequestAction.ACCEPT)) }
		assertEquals(ErrorCode.BLOCKED_MEMBER, exception.errorCode)
		assertEquals(FriendRequestStatus.PENDING, friend.status)
	}

	@Test fun `does not check for a block when rejecting`() {
		val friend = pendingRequest()
		every { friendRepository.findById(10L) } returns Optional.of(friend)

		service.execute(2L, 10L, RespondFriendRequestRequest(FriendRequestAction.REJECT))

		io.mockk.verify(exactly = 0) { blockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(any(), any(), any(), any()) }
	}

	@Test fun `accepting notifies the requester`() {
		val friend = pendingRequest()
		every { friendRepository.findById(10L) } returns Optional.of(friend)
		mockNoBlock()

		service.execute(2L, 10L, RespondFriendRequestRequest(FriendRequestAction.ACCEPT))

		io.mockk.verify(exactly = 1) {
			notificationPublisher.publish(
				friend.requester, NotificationType.FRIEND_ACCEPTED, any(), any(), 2L, NotificationTargetType.MEMBER,
			)
		}
	}

	// A rejection is deliberately silent: the sender is never told they were turned down.
	@Test fun `rejecting notifies nobody`() {
		val friend = pendingRequest()
		every { friendRepository.findById(10L) } returns Optional.of(friend)

		service.execute(2L, 10L, RespondFriendRequestRequest(FriendRequestAction.REJECT))

		io.mockk.verify(exactly = 0) { notificationPublisher.publish(any(), any(), any(), any(), any(), any()) }
	}
}
