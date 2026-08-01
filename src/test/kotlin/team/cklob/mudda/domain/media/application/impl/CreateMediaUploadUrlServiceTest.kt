package team.cklob.mudda.domain.media.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.application.SignedUrl
import team.cklob.mudda.domain.media.domain.type.MediaType
import team.cklob.mudda.domain.media.infrastructure.MediaStorageProperties
import team.cklob.mudda.domain.media.presentation.request.CreateMediaUploadUrlRequest
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateMediaUploadUrlServiceTest {
	private val mediaStorage = mockk<MediaStorage>()
	private val properties = MediaStorageProperties(bucket = "bucket")
	private val service = CreateMediaUploadUrlService(mediaStorage, properties)

	@Test
	fun `creates an upload URL for an allowed image`() {
		val expiresAt = LocalDateTime.now().plusMinutes(10)
		every { mediaStorage.createUploadUrl(any(), "image/jpeg", 1024) } returns SignedUrl("https://upload", expiresAt)

		val response = service.execute(7, CreateMediaUploadUrlRequest(MediaType.IMAGE, "image/jpeg", 1024))

		assertTrue(response.uploadKey.startsWith("pending/7/image/"))
		assertEquals("https://upload", response.uploadUrl)
		verify(exactly = 1) { mediaStorage.createUploadUrl(response.uploadKey, "image/jpeg", 1024) }
	}

	@Test
	fun `rejects a file larger than the configured type limit`() {
		val exception = assertThrows<BusinessException> {
			service.execute(7, CreateMediaUploadUrlRequest(MediaType.IMAGE, "image/jpeg", properties.maxImageSize + 1))
		}

		assertEquals(ErrorCode.INVALID_MEDIA_UPLOAD, exception.errorCode)
		verify(exactly = 0) { mediaStorage.createUploadUrl(any(), any(), any()) }
	}
}
