package team.cklob.mudda.domain.member.domain.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import team.cklob.mudda.domain.block.domain.entity.Block
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.support.PostgresIntegrationTest
import java.time.LocalDateTime

// Exercises MemberRepository#searchSelectableByNickname against a real PostgreSQL instance -- the LIKE
// ESCAPE clause, the CASE-based ranking, and the NOT EXISTS block-exclusion subquery are all things a
// MockK-based unit test cannot verify actually compile to valid, correct SQL.
class MemberRepositorySearchIntegrationTest : PostgresIntegrationTest() {
	@Autowired private lateinit var memberRepository: MemberRepository
	@Autowired private lateinit var blockRepository: BlockRepository

	private fun member(tag: String, nickname: String? = "nick-$tag", withdrawnAt: LocalDateTime? = null) = memberRepository.saveAndFlush(
		Member(
			name = "name-$tag", nickname = nickname, email = "user-$tag@example.com",
			oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$tag",
			profileVisibility = ProfileVisibility.PUBLIC, withdrawnAt = withdrawnAt,
		),
	)

	@Test fun `excludes the viewer, withdrawn members and members without a nickname`() {
		val viewer = member("viewer", nickname = "search-target")
		val withdrawn = member("withdrawn", nickname = "search-target-2", withdrawnAt = LocalDateTime.now())
		val incomplete = member("incomplete", nickname = null)
		val target = member("target", nickname = "search-target-3")

		val page = memberRepository.searchSelectableByNickname(viewer.id!!, "search-target", PageRequest.of(0, 20))

		val ids = page.content.mapNotNull { it.id }
		assertFalse(viewer.id in ids)
		assertFalse(withdrawn.id in ids)
		assertFalse(incomplete.id in ids)
		assertTrue(target.id in ids)
	}

	@Test fun `excludes a member blocked in either direction`() {
		val viewer = member("viewer2", nickname = "block-search")
		val blockedByViewer = member("blocked-by-viewer", nickname = "block-search-2")
		val blockedViewer = member("blocked-viewer", nickname = "block-search-3")
		val stranger = member("stranger", nickname = "block-search-4")
		blockRepository.saveAndFlush(Block(blocker = viewer, blocked = blockedByViewer))
		blockRepository.saveAndFlush(Block(blocker = blockedViewer, blocked = viewer))

		val page = memberRepository.searchSelectableByNickname(viewer.id!!, "block-search", PageRequest.of(0, 20))

		val ids = page.content.mapNotNull { it.id }
		assertFalse(blockedByViewer.id in ids)
		assertFalse(blockedViewer.id in ids)
		assertTrue(stranger.id in ids)
	}

	@Test fun `ranks an exact match first and a prefix match before a contains-only match`() {
		val viewer = member("viewer3")
		val containsOnly = member("contains", nickname = "aa-ranktest-zz")
		val prefix = member("prefix", nickname = "ranktest-suffix")
		val exact = member("exact", nickname = "ranktest")

		val page = memberRepository.searchSelectableByNickname(viewer.id!!, "ranktest", PageRequest.of(0, 20))

		assertEquals(listOf(exact.id, prefix.id, containsOnly.id), page.content.mapNotNull { it.id })
	}

	@Test fun `escapes LIKE wildcard characters in the keyword`() {
		val viewer = member("viewer4")
		val literalMatch = member("literal", nickname = "50%_off")
		member("decoy", nickname = "50xyoff")

		// Calling the 3-arg overload here (rather than pre-computing the escaped keyword) exercises the
		// real production call path, including MemberRepository's own escaping logic.
		val page = memberRepository.searchSelectableByNickname(viewer.id!!, "50%_off", PageRequest.of(0, 20))

		assertEquals(listOf(literalMatch.id), page.content.mapNotNull { it.id })
	}

	@Test fun `respects the page size`() {
		val viewer = member("viewer5")
		repeat(3) { member("page-$it", nickname = "page-target-$it") }

		val page = memberRepository.searchSelectableByNickname(viewer.id!!, "page-target", PageRequest.of(0, 2))

		assertEquals(2, page.content.size)
		assertEquals(3L, page.totalElements)
	}
}
