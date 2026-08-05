package team.cklob.mudda.domain.media.application.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.application.MediaUploadKey
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.domain.media.infrastructure.MediaStorageProperties
import team.cklob.mudda.domain.media.presentation.request.CompleteMediaUploadRequest
import team.cklob.mudda.domain.media.presentation.response.CompleteMediaUploadResponse
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class CompleteMediaUploadService(
	private val mediaRepository: MediaRepository,
	private val memberRepository: MemberRepository,
	private val mediaStorage: MediaStorage,
	private val properties: MediaStorageProperties,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	fun execute(memberId: Long, request: CompleteMediaUploadRequest): CompleteMediaUploadResponse {
		val key = MediaUploadKey.parse(request.uploadKey)
			?.takeIf { it.memberId == memberId }
			?: throw BusinessException(ErrorCode.INVALID_MEDIA_UPLOAD)

		mediaRepository.findByS3KeyAndUploaderId(key.permanentKey, memberId)?.let {
			return CompleteMediaUploadResponse.from(it, mediaStorage.createAccessUrl(it.s3Key))
		}

		val storedObject = mediaStorage.inspect(key.pendingKey)
		val maxSize = properties.maxSizeFor(key.mediaType)
		if (storedObject.contentType?.lowercase() !in properties.allowedContentTypesFor(key.mediaType) ||
			storedObject.contentLength <= 0 || storedObject.contentLength > maxSize
		) {
			throw BusinessException(ErrorCode.INVALID_MEDIA_UPLOAD)
		}

		memberRepository.findById(memberId).orElseThrow { AuthException(ErrorCode.UNAUTHORIZED) }
		val inserted = mediaRepository.insertUnattached(memberId, key.mediaType.name, key.permanentKey)
		if (inserted == 0) {
			val existing = mediaRepository.findByS3KeyAndUploaderId(key.permanentKey, memberId)
				?: throw BusinessException(ErrorCode.INVALID_MEDIA_UPLOAD)
			return CompleteMediaUploadResponse.from(existing, mediaStorage.createAccessUrl(existing.s3Key))
		}

		mediaStorage.copy(key.pendingKey, key.permanentKey)
		try {
			val accessUrl = mediaStorage.createAccessUrl(key.permanentKey)
			val media = mediaRepository.findByS3KeyAndUploaderId(key.permanentKey, memberId)
				?: throw BusinessException(ErrorCode.INVALID_MEDIA_UPLOAD)
			try {
				mediaStorage.delete(key.pendingKey)
			} catch (_: BusinessException) {
				logger.warn("Pending media cleanup failed; S3 lifecycle will retry cleanup")
			}
			return CompleteMediaUploadResponse.from(media, accessUrl)
		} catch (exception: Exception) {
			try {
				mediaStorage.delete(key.permanentKey)
			} catch (_: BusinessException) {
				logger.error("Permanent media compensation cleanup failed")
			}
			throw exception
		}
	}
}
