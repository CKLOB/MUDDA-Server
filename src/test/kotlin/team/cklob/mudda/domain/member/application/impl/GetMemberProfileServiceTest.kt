package team.cklob.mudda.domain.member.application.impl

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import java.util.Optional

class GetMemberProfileServiceTest {
	private val memberRepository = mockk<MemberRepository>()
	private val friendRepository = mockk<FriendRepository>()
	private val service = GetMemberProfileService(memberRepository, friendRepository)

	private fun member(id: Long, visibility: ProfileVisibility, withdrawnAt: LocalDateTime? = null, nickname: String? = "nickname-$id") = Member(
		name = "name-$id", nickname = nickname, email = "user$id@example.com",
		oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$id",
		gender = Gender.MALE, birthYear = 2000,
		profileVisibility = visibility, withdrawnAt = withdrawnAt, id = id,
	)

	private fun mockNoFriendRelation() {
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns Optional.empty()
	}

	@Test fun `returns a PUBLIC profile for another member`() {
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.PUBLIC))
		mockNoFriendRelation()

		val response = service.execute(1L, 2L)

		assertEquals(2L, response.memberId)
		assertEquals(FriendStatus.NONE, response.friendStatus)
	}

	@Test fun `allows viewing the caller's own PRIVATE profile`() {
		every { memberRepository.findById(1L) } returns Optional.of(member(1L, ProfileVisibility.PRIVATE))

		val response = service.execute(1L, 1L)

		assertEquals(1L, response.memberId)
		assertEquals(FriendStatus.NONE, response.friendStatus)
	}

	@Test fun `denies another member's PRIVATE profile`() {
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.PRIVATE))
		mockNoFriendRelation()

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, 2L) }
		assertEquals(ErrorCode.PROFILE_ACCESS_DENIED, exception.errorCode)
	}

	@Test fun `allows a friend to view a FRIEND-visibility profile`() {
		val friend = Friend(requester = member(1L, ProfileVisibility.PUBLIC), receiver = member(2L, ProfileVisibility.FRIEND), status = FriendRequestStatus.ACCEPTED, id = 10L)
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.FRIEND))
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns Optional.of(friend)

		val response = service.execute(1L, 2L)

		assertEquals(FriendStatus.FRIEND, response.friendStatus)
	}

	@Test fun `denies a non-friend viewing a FRIEND-visibility profile`() {
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.FRIEND))
		mockNoFriendRelation()

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, 2L) }
		assertEquals(ErrorCode.PROFILE_ACCESS_DENIED, exception.errorCode)
	}

	@Test fun `returns REQUESTED when the viewer sent the pending friend request`() {
		val friend = Friend(requester = member(1L, ProfileVisibility.PUBLIC), receiver = member(2L, ProfileVisibility.PUBLIC), status = FriendRequestStatus.PENDING, id = 10L)
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.PUBLIC))
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns Optional.of(friend)

		val response = service.execute(1L, 2L)

		assertEquals(FriendStatus.REQUESTED, response.friendStatus)
	}

	@Test fun `returns RECEIVED when the viewer received the pending friend request`() {
		val friend = Friend(requester = member(2L, ProfileVisibility.PUBLIC), receiver = member(1L, ProfileVisibility.PUBLIC), status = FriendRequestStatus.PENDING, id = 10L)
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.PUBLIC))
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns Optional.of(friend)

		val response = service.execute(1L, 2L)

		assertEquals(FriendStatus.RECEIVED, response.friendStatus)
	}

	@Test fun `returns FRIEND for an accepted relationship`() {
		val friend = Friend(requester = member(1L, ProfileVisibility.PUBLIC), receiver = member(2L, ProfileVisibility.PUBLIC), status = FriendRequestStatus.ACCEPTED, id = 10L)
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.PUBLIC))
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns Optional.of(friend)

		val response = service.execute(1L, 2L)

		assertEquals(FriendStatus.FRIEND, response.friendStatus)
	}

	@Test fun `returns NONE when no friend relationship exists`() {
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.PUBLIC))
		mockNoFriendRelation()

		val response = service.execute(1L, 2L)

		assertEquals(FriendStatus.NONE, response.friendStatus)
	}

	@Test fun `returns NONE when the prior request was rejected`() {
		val friend = Friend(requester = member(1L, ProfileVisibility.PUBLIC), receiver = member(2L, ProfileVisibility.PUBLIC), status = FriendRequestStatus.REJECTED, id = 10L)
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.PUBLIC))
		every { friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(1L, 2L, 2L, 1L) } returns Optional.of(friend)

		val response = service.execute(1L, 2L)

		assertEquals(FriendStatus.NONE, response.friendStatus)
	}

	@Test fun `rejects a withdrawn member's profile`() {
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.PUBLIC, withdrawnAt = LocalDateTime.now()))

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, 2L) }
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.errorCode)
	}

	@Test fun `rejects a member that has not completed signup`() {
		every { memberRepository.findById(2L) } returns Optional.of(member(2L, ProfileVisibility.PUBLIC, nickname = null))

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, 2L) }
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.errorCode)
	}

	@Test fun `rejects a memberId that does not exist`() {
		every { memberRepository.findById(2L) } returns Optional.empty()

		val exception = assertThrows(BusinessException::class.java) { service.execute(1L, 2L) }
		assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.errorCode)
	}
}
