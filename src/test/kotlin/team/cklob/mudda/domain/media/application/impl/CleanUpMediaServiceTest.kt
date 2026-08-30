package team.cklob.mudda.domain.media.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.domain.entity.Media
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.domain.media.domain.type.MediaType
import team.cklob.mudda.domain.media.infrastructure.MediaStorageProperties
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import kotlin.test.assertEquals

class CleanUpMediaServiceTest {
	private val mediaRepository = mockk<MediaRepository>()
	private val mediaStorage = mockk<MediaStorage>()
	private val service = CleanUpMediaService(mediaRepository, mediaStorage, MediaStorageProperties(bucket = "bucket"))

	private val uploader = Member(
		name = "name", nickname = "nick", email = "a@example.com", oauthProvider = OAuthProvider.GOOGLE,
		providerId = "provider", profileVisibility = ProfileVisibility.PUBLIC, id = 1,
	)

	private fun media(id: Long, key: String) = Media(uploader, null, MediaType.IMAGE, key, id)

	@Test fun `nothing is deleted when there are no orphans`() {
		every { mediaRepository.findUnattachedOlderThan(any(), any()) } returns emptyList()

		assertEquals(0, service.execute())

		verify(exactly = 0) { mediaStorage.delete(any()) }
	}

	// The row is the only pointer to the object, so dropping it after a failed storage delete would strand
	// the object with nothing left to find it by.
	@Test fun `a row whose object could not be deleted is kept for the next run`() {
		val ok = media(1, "key-ok")
		val failing = media(2, "key-failing")
		every { mediaRepository.findUnattachedOlderThan(any(), any()) } returns listOf(ok, failing)
		every { mediaStorage.delete("key-ok") } returns Unit
		every { mediaStorage.delete("key-failing") } throws BusinessException(ErrorCode.MEDIA_STORAGE_ERROR)
		val deleted = slot<List<Media>>()
		every { mediaRepository.deleteAll(capture(deleted)) } returns Unit

		assertEquals(1, service.execute())

		assertEquals(listOf(ok), deleted.captured)
	}

	@Test fun `every orphan whose object is gone has its row removed`() {
		val orphans = listOf(media(1, "a"), media(2, "b"))
		every { mediaRepository.findUnattachedOlderThan(any(), any()) } returns orphans
		every { mediaStorage.delete(any()) } returns Unit
		val deleted = slot<List<Media>>()
		every { mediaRepository.deleteAll(capture(deleted)) } returns Unit

		assertEquals(2, service.execute())

		assertEquals(orphans, deleted.captured)
	}
}
