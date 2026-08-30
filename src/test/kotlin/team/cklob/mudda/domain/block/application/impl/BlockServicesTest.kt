package team.cklob.mudda.domain.block.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import team.cklob.mudda.domain.block.domain.entity.Block
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.block.presentation.request.CreateBlockRequest
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

class BlockServicesTest {
	private val blockRepository = mockk<BlockRepository>()
	private val memberRepository = mockk<MemberRepository>()
	private val createService = CreateBlockService(blockRepository, memberRepository)
	private val deleteService = DeleteBlockService(blockRepository)

	private fun member(id: Long, withdrawnAt: LocalDateTime? = null) = Member(
		name = "name", nickname = "nick$id", email = "a$id@example.com", oauthProvider = OAuthProvider.GOOGLE,
		providerId = "provider$id", profileVisibility = ProfileVisibility.PUBLIC, withdrawnAt = withdrawnAt, id = id,
	)

	@Test fun `blocking yourself is rejected`() {
		val error = assertThrows<BusinessException> { createService.execute(1, CreateBlockRequest(1)) }

		assertEquals(ErrorCode.CANNOT_BLOCK_SELF, error.errorCode)
	}

	@Test fun `blocking a withdrawn member reports the member as missing`() {
		every { memberRepository.findById(2) } returns Optional.of(member(2, withdrawnAt = LocalDateTime.now()))

		val error = assertThrows<BusinessException> { createService.execute(1, CreateBlockRequest(2)) }

		assertEquals(ErrorCode.MEMBER_NOT_FOUND, error.errorCode)
	}

	// Blocking twice lands on the same end state, so the existing row is returned rather than a conflict
	// the client would have to special-case. insertIfAbsent reports 0 rows affected for the repeat, which
	// is also the path a concurrent loser takes.
	@Test fun `blocking an already blocked member is idempotent`() {
		val existing = Block(blocker = member(1), blocked = member(2), id = 9)
		every { memberRepository.findById(2) } returns Optional.of(member(2))
		every { memberRepository.existsById(1) } returns true
		every { blockRepository.insertIfAbsent(1, 2) } returns 0
		every { blockRepository.findByBlockerIdAndBlockedId(1, 2) } returns Optional.of(existing)

		val response = createService.execute(1, CreateBlockRequest(2))

		assertEquals(9, response.blockId)
	}

	// The whole block policy rests on read-path filtering: nothing is deleted, so unblocking restores the
	// prior friendship and pending requests for free.
	@Test fun `blocking writes one row and deletes nothing`() {
		every { memberRepository.findById(2) } returns Optional.of(member(2))
		every { memberRepository.existsById(1) } returns true
		every { blockRepository.insertIfAbsent(1, 2) } returns 1
		every { blockRepository.findByBlockerIdAndBlockedId(1, 2) } returns Optional.of(Block(member(1), member(2), id = 5))

		val response = createService.execute(1, CreateBlockRequest(2))

		assertEquals(5, response.blockId)
		verify(exactly = 1) { blockRepository.insertIfAbsent(1, 2) }
		verify(exactly = 0) { blockRepository.delete(any()) }
	}

	@Test fun `unblocking a member that was never blocked reports not found`() {
		every { blockRepository.findByBlockerIdAndBlockedId(1, 2) } returns Optional.empty()

		val error = assertThrows<BusinessException> { deleteService.execute(1, 2) }

		assertEquals(ErrorCode.BLOCK_NOT_FOUND, error.errorCode)
	}
}
