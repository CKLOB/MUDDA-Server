package team.cklob.mudda.domain.friend.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.presentation.request.SendFriendRequestRequest
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.notification.application.impl.NotificationPublisher
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import java.util.Optional

class SendFriendRequestServiceTest {
	private val friendRepository = mockk<FriendRepository>()
	private val memberRepository = mockk<MemberRepository>()
	private val blockRepository = mockk<BlockRepository>()
	private val notificationPublisher = mockk<NotificationPublisher>(relaxed = true)
	private val service = SendFriendRequestService(friendRepository, memberRepository, blockRepository, notificationPublisher)

	private fun member(id: Long, withdrawnAt: LocalDateTime? = null, nickname: String? = "nickname-$id") = Member(
		name = "name-$id", nickname = nickname, email = "user$id@example.com",
		oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$id",
		profileVisibility = ProfileVisibility.PUBLIC, withdrawnAt = withdrawnAt, id = id,
	)

	private fun mockNoBlock() {
		every { blockRepository.existsByBlockerIdAndBlockedId(1L, 2L) } returns false
		every { blockRepository.existsByBlockerIdAndBlockedId(2L, 1L) } returns false
	}

	private fun mockNoExistingRelation() {
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns emptyList()
	}

	@Test fun `creates a pending request when there is no prior relationship`() {
		val requester = member(1L)
		val receiver = member(2L)
		every { memberRepository.findById(1L) } returns Optional.of(requester)
		every { memberRepository.findById(2L) } returns Optional.of(receiver)
		mockNoBlock()
		mockNoExistingRelation()
		val savedSlot = slot<Friend>()
		every { friendRepository.saveAndFlush(capture(savedSlot)) } answers { Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.PENDING, id = 10L) }

		val response = service.execute(1L, SendFriendRequestRequest(receiverId = 2L))

		assertEquals(10L, response.requestId)
		assertEquals(FriendRequestStatus.PENDING, response.status)
		assertEquals(1L, savedSlot.captured.requester.id)
		assertEquals(2L, savedSlot.captured.receiver.id)
	}

	@Test fun `rejects a request to yourself`() {
		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 1L)) }
		assertEquals(ErrorCode.CANNOT_REQUEST_SELF, exception.errorCode)
	}

	@Test fun `rejects when the requester id does not exist`() {
		every { memberRepository.findById(1L) } returns Optional.empty()

		val exception = assertThrows(AuthException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }
		assertEquals(ErrorCode.UNAUTHORIZED, exception.errorCode)
	}

	@Test fun `rejects when the receiver does not exist`() {
		every { memberRepository.findById(1L) } returns Optional.of(member(1L))
		every { memberRepository.findById(2L) } returns Optional.empty()

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.errorCode)
	}

	@Test fun `rejects when the receiver has withdrawn`() {
		every { memberRepository.findById(1L) } returns Optional.of(member(1L))
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, withdrawnAt = LocalDateTime.now()))

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.errorCode)
	}

	@Test fun `rejects when already friends`() {
		val requester = member(1L)
		val receiver = member(2L)
		every { memberRepository.findById(1L) } returns Optional.of(requester)
		every { memberRepository.findById(2L) } returns Optional.of(receiver)
		mockNoBlock()
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns
			listOf(Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.ACCEPTED, id = 5L))

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }
		assertEquals(ErrorCode.ALREADY_FRIENDS, exception.errorCode)
	}

	@Test fun `rejects a duplicate same-direction pending request`() {
		val requester = member(1L)
		val receiver = member(2L)
		every { memberRepository.findById(1L) } returns Optional.of(requester)
		every { memberRepository.findById(2L) } returns Optional.of(receiver)
		mockNoBlock()
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns
			listOf(Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.PENDING, id = 5L))

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }
		assertEquals(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS, exception.errorCode)
	}

	@Test fun `rejects when the other member already sent a pending request in reverse`() {
		val requester = member(1L)
		val receiver = member(2L)
		every { memberRepository.findById(1L) } returns Optional.of(requester)
		every { memberRepository.findById(2L) } returns Optional.of(receiver)
		mockNoBlock()
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns
			listOf(Friend(requester = receiver, receiver = requester, status = FriendRequestStatus.PENDING, id = 5L))

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }
		assertEquals(ErrorCode.REVERSE_FRIEND_REQUEST_EXISTS, exception.errorCode)
	}

	@Test fun `allows a new request after a prior rejection`() {
		val requester = member(1L)
		val receiver = member(2L)
		every { memberRepository.findById(1L) } returns Optional.of(requester)
		every { memberRepository.findById(2L) } returns Optional.of(receiver)
		mockNoBlock()
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns
			listOf(Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.REJECTED, id = 5L))
		every { friendRepository.saveAndFlush(any()) } returns Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.PENDING, id = 11L)

		val response = service.execute(1L, SendFriendRequestRequest(receiverId = 2L))

		assertEquals(11L, response.requestId)
		// This only verifies the app-level check doesn't block a REJECTED relation; it can't prove the DB
		// itself allows the insert (saveAndFlush is mocked). That's covered separately -- and for real -- by
		// FriendRepositoryIntegrationTest's "a rejected request can be sent again in the same direction",
		// which exercises the actual uq_friend_requester_receiver partial index from the V4 migration.
	}

	@Test fun `rejects when the caller has blocked the target`() {
		every { memberRepository.findById(1L) } returns Optional.of(member(1L))
		every { memberRepository.findById(2L) } returns Optional.of(member(2L))
		every { blockRepository.existsByBlockerIdAndBlockedId(1L, 2L) } returns true

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }
		assertEquals(ErrorCode.BLOCKED_MEMBER, exception.errorCode)
	}

	@Test fun `hides the block as member-not-found when the target has blocked the caller`() {
		every { memberRepository.findById(1L) } returns Optional.of(member(1L))
		every { memberRepository.findById(2L) } returns Optional.of(member(2L))
		every { blockRepository.existsByBlockerIdAndBlockedId(1L, 2L) } returns false
		every { blockRepository.existsByBlockerIdAndBlockedId(2L, 1L) } returns true

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.errorCode)
	}

	@Test fun `translates a concurrent reverse-direction insert race into a conflict`() {
		val requester = member(1L)
		val receiver = member(2L)
		every { memberRepository.findById(1L) } returns Optional.of(requester)
		every { memberRepository.findById(2L) } returns Optional.of(receiver)
		mockNoBlock()
		mockNoExistingRelation()
		every { friendRepository.saveAndFlush(any()) } throws DataIntegrityViolationException("uq_friend_pending_pair")

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }
		assertEquals(ErrorCode.REVERSE_FRIEND_REQUEST_EXISTS, exception.errorCode)
	}

	@Test fun `saves the request through the repository`() {
		val requester = member(1L)
		val receiver = member(2L)
		every { memberRepository.findById(1L) } returns Optional.of(requester)
		every { memberRepository.findById(2L) } returns Optional.of(receiver)
		mockNoBlock()
		mockNoExistingRelation()
		every { friendRepository.saveAndFlush(any()) } returns Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.PENDING, id = 10L)

		service.execute(1L, SendFriendRequestRequest(receiverId = 2L))

		verify(exactly = 1) { friendRepository.saveAndFlush(any()) }
	}

	@Test fun `a created request notifies the receiver and points at the request itself`() {
		val requester = member(1L)
		val receiver = member(2L)
		every { memberRepository.findById(1L) } returns Optional.of(requester)
		every { memberRepository.findById(2L) } returns Optional.of(receiver)
		mockNoBlock()
		mockNoExistingRelation()
		every { friendRepository.saveAndFlush(any()) } answers {
			Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.PENDING, id = 10L)
		}

		service.execute(1L, SendFriendRequestRequest(receiverId = 2L))

		verify(exactly = 1) {
			notificationPublisher.publish(
				receiver, NotificationType.FRIEND_REQUESTED, any(), any(), 10L, NotificationTargetType.FRIEND_REQUEST,
			)
		}
	}

	@Test fun `a rejected request notifies nobody`() {
		val requester = member(1L)
		val receiver = member(2L)
		every { memberRepository.findById(1L) } returns Optional.of(requester)
		every { memberRepository.findById(2L) } returns Optional.of(receiver)
		mockNoBlock()
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns
			listOf(Friend(requester = requester, receiver = receiver, status = FriendRequestStatus.ACCEPTED, id = 10L))

		assertThrows(BusinessException::class.java) { service.execute(1L, SendFriendRequestRequest(receiverId = 2L)) }

		verify(exactly = 0) { notificationPublisher.publish(any(), any(), any(), any(), any(), any()) }
	}
}
