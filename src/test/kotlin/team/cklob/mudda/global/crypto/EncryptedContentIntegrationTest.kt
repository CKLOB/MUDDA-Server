package team.cklob.mudda.global.crypto

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired
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

// The unit tests prove ContentCipher is correct in isolation; this proves the converter is actually wired
// into Hibernate and that what lands in the column is ciphertext. Reading the raw column with a native
// query is the only way to make the "the server never stores capsule content in plaintext" claim
// verifiable rather than assumed -- going through JPA would decrypt it right back.
class EncryptedContentIntegrationTest : PostgresIntegrationTest() {
    @Autowired private lateinit var timeCapsuleRepository: TimeCapsuleRepository
    @Autowired private lateinit var memberRepository: MemberRepository
    @Autowired private lateinit var entityManager: EntityManager

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private fun member(tag: String) = memberRepository.saveAndFlush(
        Member(
            name = "name-$tag", nickname = "nickname-$tag", email = "user-$tag@example.com",
            oauthProvider = OAuthProvider.GOOGLE, providerId = "google-sub-$tag", profileVisibility = ProfileVisibility.PUBLIC,
        ),
    )

    private fun point(): Point = geometryFactory.createPoint(Coordinate(126.9780, 37.5665))

    private fun capsule(tag: String, content: String?) = timeCapsuleRepository.saveAndFlush(
        TimeCapsule(
            member = member(tag),
            name = "capsule-$tag",
            content = content,
            timeCapsuleType = "TEXT",
            visibility = CapsuleVisibility.PRIVATE,
            lockType = CapsuleLockType.NONE,
            location = point(),
            openRadiusMeter = 50,
            expiresAt = LocalDateTime.now().plusYears(1),
            isAnonymous = false,
            isFeedPublic = false,
        ),
    )

    private fun rawContentOf(id: Long): String? = entityManager
        .createNativeQuery("SELECT content FROM tbl_time_capsule WHERE id = :id")
        .setParameter("id", id)
        .singleResult as String?

    @Test fun `stores capsule content as ciphertext and never as plaintext`() {
        val plaintext = "10년 뒤의 나에게 남기는 비밀"
        val saved = capsule("secret", plaintext)
        entityManager.flush()

        val raw = requireNotNull(rawContentOf(saved.id!!))

        assertFalse(raw.contains(plaintext), "raw column still contained the plaintext body")
        assertTrue(raw.startsWith("v1:"), "raw column was not a versioned cipher envelope: $raw")
    }

    @Test fun `decrypts capsule content back to the original on read`() {
        val plaintext = "여기 묻어둔 이야기 🎁"
        val saved = capsule("roundtrip", plaintext)
        entityManager.flush()
        entityManager.clear()

        val reloaded = timeCapsuleRepository.findById(saved.id!!).orElseThrow()

        assertEquals(plaintext, reloaded.content)
    }

    @Test fun `leaves a null content untouched`() {
        val saved = capsule("empty", null)
        entityManager.flush()
        entityManager.clear()

        assertNull(rawContentOf(saved.id!!))
        assertNull(timeCapsuleRepository.findById(saved.id!!).orElseThrow().content)
    }

    @Test fun `produces a different ciphertext for two capsules with identical content`() {
        val plaintext = "동일한 본문"
        val first = capsule("same-a", plaintext)
        val second = capsule("same-b", plaintext)
        entityManager.flush()

        assertFalse(rawContentOf(first.id!!) == rawContentOf(second.id!!))
    }
}
