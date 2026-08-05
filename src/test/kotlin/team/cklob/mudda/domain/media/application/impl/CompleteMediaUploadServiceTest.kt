package team.cklob.mudda.domain.media.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.application.MediaUploadKey
import team.cklob.mudda.domain.media.application.SignedUrl
import team.cklob.mudda.domain.media.application.StoredObject
import team.cklob.mudda.domain.media.domain.entity.Media
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.domain.media.domain.type.MediaType
import team.cklob.mudda.domain.media.infrastructure.MediaStorageProperties
import team.cklob.mudda.domain.media.presentation.request.CompleteMediaUploadRequest
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

class CompleteMediaUploadServiceTest {
	private val mediaRepository = mockk<MediaRepository>()
	private val memberRepository = mockk<MemberRepository>()
	private val mediaStorage = mockk<MediaStorage>()
	private val properties = MediaStorageProperties(bucket = "bucket")
	private val service = CompleteMediaUploadService(mediaRepository, memberRepository, mediaStorage, properties)
	private val member = mockk<Member>()
	private val accessUrl = SignedUrl("https://access", LocalDateTime.now().plusMinutes(5))

	@Test
	fun `promotes and registers a valid pending upload`() {
		val key = MediaUploadKey.create(7, MediaType.IMAGE)
		every { mediaStorage.inspect(key.pendingKey) } returns StoredObject("image/jpeg", 1024)
		every { memberRepository.findById(7) } returns Optional.of(member)
		every { mediaRepository.insertUnattached(7, "IMAGE", key.permanentKey) } returns 1
		every { mediaStorage.copy(key.pendingKey, key.permanentKey) } returns Unit
		every { mediaStorage.createAccessUrl(key.permanentKey) } returns accessUrl
		every { mediaRepository.findByS3KeyAndUploaderId(key.permanentKey, 7) } returnsMany listOf(
			null,
			Media(uploader = member, mediaType = MediaType.IMAGE, s3Key = key.permanentKey, id = 11),
		)
		every { mediaStorage.delete(key.pendingKey) } returns Unit

		val response = service.execute(7, CompleteMediaUploadRequest(key.pendingKey))

		assertEquals(11, response.mediaId)
		assertEquals("https://access", response.accessUrl)
		verify(ordering = io.mockk.Ordering.ORDERED) {
			mediaRepository.insertUnattached(7, "IMAGE", key.permanentKey)
			mediaStorage.copy(key.pendingKey, key.permanentKey)
			mediaStorage.delete(key.pendingKey)
		}
	}

	@Test
	fun `returns an existing record when completion is retried`() {
		val key = MediaUploadKey.create(7, MediaType.VIDEO)
		val media = Media(uploader = member, mediaType = MediaType.VIDEO, s3Key = key.permanentKey, id = 12)
		every { mediaRepository.findByS3KeyAndUploaderId(key.permanentKey, 7) } returns media
		every { mediaStorage.createAccessUrl(key.permanentKey) } returns accessUrl

		val response = service.execute(7, CompleteMediaUploadRequest(key.pendingKey))

		assertEquals(12, response.mediaId)
		verify(exactly = 0) { mediaStorage.inspect(any()) }
	}

	@Test
	fun `does not promote when another request wins the registration race`() {
		val key = MediaUploadKey.create(7, MediaType.IMAGE)
		val media = Media(uploader = member, mediaType = MediaType.IMAGE, s3Key = key.permanentKey, id = 13)
		every { mediaRepository.findByS3KeyAndUploaderId(key.permanentKey, 7) } returnsMany listOf(null, media)
		every { mediaStorage.inspect(key.pendingKey) } returns StoredObject("image/jpeg", 1024)
		every { memberRepository.findById(7) } returns Optional.of(member)
		every { mediaRepository.insertUnattached(7, "IMAGE", key.permanentKey) } returns 0
		every { mediaStorage.createAccessUrl(key.permanentKey) } returns accessUrl

		val response = service.execute(7, CompleteMediaUploadRequest(key.pendingKey))

		assertEquals(13, response.mediaId)
		verify(exactly = 0) { mediaStorage.copy(any(), any()) }
	}

	@Test
	fun `rejects an upload key owned by another member`() {
		val key = MediaUploadKey.create(8, MediaType.IMAGE)

		val exception = assertThrows<BusinessException> {
			service.execute(7, CompleteMediaUploadRequest(key.pendingKey))
		}

		assertEquals(ErrorCode.INVALID_MEDIA_UPLOAD, exception.errorCode)
		verify(exactly = 0) { mediaRepository.findByS3KeyAndUploaderId(any(), any()) }
	}

	@Test
	fun `removes the promoted object when the inserted media cannot be read`() {
		val key = MediaUploadKey.create(7, MediaType.IMAGE)
		every { mediaRepository.findByS3KeyAndUploaderId(key.permanentKey, 7) } returns null
		every { mediaStorage.inspect(key.pendingKey) } returns StoredObject("image/jpeg", 1024)
		every { memberRepository.findById(7) } returns Optional.of(member)
		every { mediaRepository.insertUnattached(7, "IMAGE", key.permanentKey) } returns 1
		every { mediaStorage.copy(key.pendingKey, key.permanentKey) } returns Unit
		every { mediaStorage.createAccessUrl(key.permanentKey) } returns accessUrl
		every { mediaRepository.findByS3KeyAndUploaderId(key.permanentKey, 7) } returnsMany listOf(null, null)
		every { mediaStorage.delete(key.permanentKey) } returns Unit

		assertThrows<BusinessException> {
			service.execute(7, CompleteMediaUploadRequest(key.pendingKey))
		}

		verify(exactly = 1) { mediaStorage.delete(key.permanentKey) }
	}
}
