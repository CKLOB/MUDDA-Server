package team.cklob.mudda.domain.feed.application.impl

import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import team.cklob.mudda.domain.feed.domain.type.FeedType
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleOpen
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleOpenRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.support.PostgresIntegrationTest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The feed's visibility filter is the whole privacy boundary of the endpoint, so it is verified against
// the real schema rather than a mocked repository.
class FeedQueryIntegrationTest(
	@Autowired private val capsuleOpenRepository: CapsuleOpenRepository,
	@Autowired private val capsuleRepository: TimeCapsuleRepository,
	@Autowired private val memberRepository: MemberRepository,
	@Autowired private val service: GetFeedListService,
) : PostgresIntegrationTest() {
	private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

	private fun member(tag: String) = memberRepository.saveAndFlush(
		Member(
			name = "name-$tag", nickname = "nick-$tag", email = "feed-$tag@example.com",
			oauthProvider = OAuthProvider.GOOGLE, providerId = "feed-provider-$tag",
			profileVisibility = ProfileVisibility.PUBLIC,
		),
	)

	private fun capsule(owner: Member, name: String, visibility: CapsuleVisibility, isDeleted: Boolean = false) =
		capsuleRepository.saveAndFlush(
			TimeCapsule(
				member = owner, name = name, content = "content", visibility = visibility,
				lockType = CapsuleLockType.NONE,
				location = geometryFactory.createPoint(Coordinate(127.0, 37.5)),
				openRadiusMeter = 100, openAt = LocalDateTime.now().minusDays(1), isDeleted = isDeleted,
			),
		)

	private fun open(capsule: TimeCapsule, opener: Member, openedAt: LocalDateTime) =
		capsuleOpenRepository.saveAndFlush(CapsuleOpen(capsule, opener, openedAt))

	@Test fun `only public undeleted capsules reach the feed`() {
		val owner = member("owner")
		val opener = member("opener")
		val now = LocalDateTime.now()
		open(capsule(owner, "public", CapsuleVisibility.PUBLIC), opener, now)
		open(capsule(owner, "private", CapsuleVisibility.PRIVATE), opener, now)
		open(capsule(owner, "friend", CapsuleVisibility.FRIEND), opener, now)
		open(capsule(owner, "deleted", CapsuleVisibility.PUBLIC, isDeleted = true), opener, now)

		val feeds = service.execute(PageRequest.of(0, 20)).feeds

		assertEquals(1, feeds.size)
		assertEquals(FeedType.CAPSULE_OPENED, feeds.first().type)
		assertTrue(feeds.first().message.contains("public"))
		assertEquals(requireNotNull(opener.id), feeds.first().member.memberId)
	}

	@Test fun `the feed is ordered newest first`() {
		val owner = member("order-owner")
		val opener = member("order-opener")
		val older = capsule(owner, "older", CapsuleVisibility.PUBLIC)
		val newer = capsule(owner, "newer", CapsuleVisibility.PUBLIC)
		open(older, opener, LocalDateTime.now().minusHours(2))
		open(newer, opener, LocalDateTime.now().minusMinutes(5))

		val feeds = service.execute(PageRequest.of(0, 20)).feeds

		assertEquals(listOf(requireNotNull(newer.id), requireNotNull(older.id)), feeds.map { it.capsuleId })
	}
}
