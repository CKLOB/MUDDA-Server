package team.cklob.mudda.domain.media.domain.repository

import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import team.cklob.mudda.domain.media.domain.entity.Media
import team.cklob.mudda.domain.media.domain.type.MediaType
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.support.PostgresIntegrationTest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Both sides of the cleanup/attach race take a pessimistic lock, so these queries are exercised against
// real PostgreSQL: a malformed FOR UPDATE (or one combined illegally with the page limit) only fails at
// runtime, never at compile time.
class MediaCleanupLockIntegrationTest(
	@Autowired private val mediaRepository: MediaRepository,
	@Autowired private val memberRepository: MemberRepository,
	@Autowired private val capsuleRepository: TimeCapsuleRepository,
) : PostgresIntegrationTest() {
	private val old = LocalDateTime.now().minusDays(7)

	private fun member(tag: String) = memberRepository.saveAndFlush(
		Member(
			name = "name", nickname = "nick-$tag", email = "media-$tag@example.com",
			oauthProvider = OAuthProvider.GOOGLE, providerId = "media-provider-$tag",
			profileVisibility = ProfileVisibility.PUBLIC,
		),
	)

	private fun capsule(owner: Member) = capsuleRepository.saveAndFlush(
		TimeCapsule(
			member = owner, name = "capsule", visibility = CapsuleVisibility.PUBLIC, lockType = CapsuleLockType.NONE,
			location = GeometryFactory(PrecisionModel(), 4326).createPoint(Coordinate(127.0, 37.5)),
			openRadiusMeter = 100, openAt = LocalDateTime.now(),
		),
	)

	private fun media(uploader: Member, key: String, capsule: TimeCapsule? = null) =
		mediaRepository.saveAndFlush(Media(uploader, capsule, MediaType.IMAGE, key))

	@Test fun `the locking cleanup query returns only unattached rows`() {
		val uploader = member("a")
		val orphan = media(uploader, "orphan-a")
		media(uploader, "attached-a", capsule(uploader))

		val found = mediaRepository.findUnattachedOlderThan(LocalDateTime.now().plusMinutes(1), PageRequest.of(0, 500))

		val ids = found.map { it.id }
		assertTrue(orphan.id in ids)
		assertTrue(found.all { it.timeCapsule == null }, "an attached row must never be a cleanup candidate")
	}

	@Test fun `the cleanup query respects the batch limit while locking`() {
		val uploader = member("b")
		repeat(3) { media(uploader, "batch-b-$it") }

		val found = mediaRepository.findUnattachedOlderThan(LocalDateTime.now().plusMinutes(1), PageRequest.of(0, 2))

		assertEquals(2, found.size)
	}

	@Test fun `the cleanup query ignores rows newer than the threshold`() {
		val uploader = member("c")
		media(uploader, "fresh-c")

		val found = mediaRepository.findUnattachedOlderThan(old, PageRequest.of(0, 500))

		assertTrue(found.none { it.s3Key == "fresh-c" })
	}

	@Test fun `the attach-side locking load returns the requested rows`() {
		val uploader = member("d")
		val first = media(uploader, "attach-d-1")
		val second = media(uploader, "attach-d-2")

		val found = mediaRepository.findAllByIdForUpdate(listOf(requireNotNull(first.id), requireNotNull(second.id)))

		assertEquals(2, found.size)
	}

	@Test fun `the attach-side locking load tolerates an empty id set`() {
		assertTrue(mediaRepository.findAllByIdForUpdate(emptyList()).isEmpty())
	}
}
