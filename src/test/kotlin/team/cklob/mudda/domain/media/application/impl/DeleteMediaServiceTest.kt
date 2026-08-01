package team.cklob.mudda.domain.media.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.domain.entity.Media
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import kotlin.test.assertEquals

class DeleteMediaServiceTest {
	private val mediaRepository = mockk<MediaRepository>()
	private val mediaStorage = mockk<MediaStorage>()
	private val service = DeleteMediaService(mediaRepository, mediaStorage)

	@Test
	fun `deletes an unattached media owned by the member`() {
		val media = mockk<Media> {
			every { timeCapsule } returns null
			every { s3Key } returns "media/7/image/id"
		}
		every { mediaRepository.findByIdAndUploaderId(1, 7) } returns media
		every { mediaStorage.delete("media/7/image/id") } returns Unit
		every { mediaRepository.delete(media) } returns Unit

		service.execute(7, 1)

		verify(ordering = io.mockk.Ordering.ORDERED) {
			mediaStorage.delete("media/7/image/id")
			mediaRepository.delete(media)
		}
	}

	@Test
	fun `hides media owned by another member as not found`() {
		every { mediaRepository.findByIdAndUploaderId(1, 7) } returns null

		val exception = assertThrows<BusinessException> { service.execute(7, 1) }

		assertEquals(ErrorCode.MEDIA_NOT_FOUND, exception.errorCode)
	}

	@Test
	fun `rejects deletion after media is attached`() {
		val media = mockk<Media> {
			every { timeCapsule } returns mockk()
		}
		every { mediaRepository.findByIdAndUploaderId(1, 7) } returns media

		val exception = assertThrows<BusinessException> { service.execute(7, 1) }

		assertEquals(ErrorCode.MEDIA_ALREADY_ATTACHED, exception.errorCode)
		verify(exactly = 0) { mediaStorage.delete(any()) }
	}
}
