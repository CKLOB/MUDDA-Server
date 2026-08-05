package team.cklob.mudda.domain.friend.domain.repository

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import team.cklob.mudda.domain.friend.domain.entity.Friend
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility

class PostgisContainer(imageName: DockerImageName) : PostgreSQLContainer<PostgisContainer>(imageName)

// Exercises the real PostgreSQL/PostGIS schema produced by the actual Flyway migrations (including the
// new V4 pending-pair unique index), which a MockK-based unit test cannot verify.
@SpringBootTest(
	properties = [
		"spring.cloud.aws.region.static=ap-northeast-2",
		"spring.cloud.aws.credentials.access-key=test",
		"spring.cloud.aws.credentials.secret-key=test",
		"jwt.secret=local-test-secret-must-be-at-least-32-bytes",
	],
)
@Testcontainers
@Transactional
class FriendRepositoryIntegrationTest {
	@Autowired private lateinit var friendRepository: FriendRepository
	@Autowired private lateinit var memberRepository: MemberRepository
	@Autowired private lateinit var entityManager: EntityManager

	private fun member(tag: String) = memberRepository.saveAndFlush(
		Member(
			name = "name-$tag", nickname = "nickname-$tag", email = "user-$tag@example.com",
			oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$tag", profileVisibility = ProfileVisibility.PUBLIC,
		),
	)

	@Test fun `finds a relationship regardless of which side is the requester`() {
		val a = member("a")
		val b = member("b")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.PENDING))

		val fromAsFirst = friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(a.id!!, b.id!!, b.id!!, a.id!!)
		val fromBAsFirst = friendRepository.findByRequesterIdAndReceiverIdOrRequesterIdAndReceiverId(b.id!!, a.id!!, a.id!!, b.id!!)

		assertEquals(1, fromAsFirst.size)
		assertEquals(1, fromBAsFirst.size)
	}

	@Test fun `findFriendships returns ACCEPTED relationships from either direction`() {
		val a = member("a")
		val b = member("b")
		val c = member("c")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.ACCEPTED, acceptedAt = java.time.LocalDateTime.now()))
		friendRepository.saveAndFlush(Friend(requester = c, receiver = a, status = FriendRequestStatus.ACCEPTED, acceptedAt = java.time.LocalDateTime.now()))
		friendRepository.saveAndFlush(Friend(requester = a, receiver = member("d"), status = FriendRequestStatus.PENDING))
		entityManager.flush()
		entityManager.clear()

		val page = friendRepository.findFriendships(a.id!!, FriendRequestStatus.ACCEPTED, PageRequest.of(0, 20))

		assertEquals(2, page.totalElements)
		assertTrue(page.content.all { it.status == FriendRequestStatus.ACCEPTED })
	}

	@Test fun `same-direction duplicate PENDING request is rejected by the unique constraint`() {
		val a = member("a")
		val b = member("b")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.PENDING))

		assertThrows(DataIntegrityViolationException::class.java) {
			friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.PENDING))
		}
	}

	@Test fun `reverse-direction concurrent PENDING request is rejected by uq_friend_pending_pair`() {
		val a = member("a")
		val b = member("b")
		friendRepository.saveAndFlush(Friend(requester = a, receiver = b, status = FriendRequestStatus.PENDING))

		assertThrows(DataIntegrityViolationException::class.java) {
			friendRepository.saveAndFlush(Friend(requester = b, receiver = a, status = FriendRequestStatus.PENDING))
		}
	}

	companion object {
		private val postgisImage = DockerImageName
			.parse("postgis/postgis:16-3.5-alpine")
			.asCompatibleSubstituteFor("postgres")

		@Container
		@ServiceConnection
		@JvmStatic
		val postgres = PostgisContainer(postgisImage)
			.withInitScript("db/init/001_enable_postgis.sql")
	}
}
