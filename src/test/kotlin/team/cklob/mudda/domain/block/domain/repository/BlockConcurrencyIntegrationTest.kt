package team.cklob.mudda.domain.block.domain.repository

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.support.PostgresIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The idempotent block contract rests on an atomic insert rather than a read-then-write check, so it is
// verified against the real uq_block_blocker_blocked constraint.
class BlockConcurrencyIntegrationTest(
	@Autowired private val blockRepository: BlockRepository,
	@Autowired private val memberRepository: MemberRepository,
) : PostgresIntegrationTest() {
	private fun member(tag: String) = memberRepository.saveAndFlush(
		Member(
			name = "name", nickname = "nick-$tag", email = "block-$tag@example.com",
			oauthProvider = OAuthProvider.GOOGLE, providerId = "block-provider-$tag",
			profileVisibility = ProfileVisibility.PUBLIC,
		),
	)

	@Test fun `the first insert creates a row and reports one affected`() {
		val blocker = member("a")
		val blocked = member("b")

		val affected = blockRepository.insertIfAbsent(requireNotNull(blocker.id), requireNotNull(blocked.id))

		assertEquals(1, affected)
		assertTrue(blockRepository.findByBlockerIdAndBlockedId(requireNotNull(blocker.id), requireNotNull(blocked.id)).isPresent)
	}

	// This is the path a concurrent loser takes: the unique constraint would otherwise surface as a
	// DataIntegrityViolationException and a 500.
	@Test fun `a repeated insert affects nothing instead of violating the unique constraint`() {
		val blocker = member("c")
		val blocked = member("d")
		blockRepository.insertIfAbsent(requireNotNull(blocker.id), requireNotNull(blocked.id))

		val affected = blockRepository.insertIfAbsent(requireNotNull(blocker.id), requireNotNull(blocked.id))

		assertEquals(0, affected)
		assertEquals(1, blockRepository.findByBlockerId(requireNotNull(blocker.id)).size)
	}

	@Test fun `blocking in the opposite direction is a separate row`() {
		val a = member("e")
		val b = member("f")
		blockRepository.insertIfAbsent(requireNotNull(a.id), requireNotNull(b.id))

		val affected = blockRepository.insertIfAbsent(requireNotNull(b.id), requireNotNull(a.id))

		assertEquals(1, affected)
	}
}
