package team.cklob.mudda.domain.media.application.impl

import org.springframework.stereotype.Service
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.application.MediaUploadKey
import team.cklob.mudda.domain.media.infrastructure.MediaStorageProperties
import team.cklob.mudda.domain.media.presentation.request.CreateMediaUploadUrlRequest
import team.cklob.mudda.domain.media.presentation.response.CreateMediaUploadUrlResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class CreateMediaUploadUrlService(
	private val mediaStorage: MediaStorage,
	private val properties: MediaStorageProperties,
) {
	fun execute(memberId: Long, request: CreateMediaUploadUrlRequest): CreateMediaUploadUrlResponse {
		validate(request)
		val key = MediaUploadKey.create(memberId, request.mediaType).pendingKey
		val signedUrl = mediaStorage.createUploadUrl(key, request.contentType, request.fileSize)
		return CreateMediaUploadUrlResponse(key, signedUrl.url, signedUrl.expiresAt)
	}

	private fun validate(request: CreateMediaUploadUrlRequest) {
		val allowedTypes = properties.allowedContentTypesFor(request.mediaType)
		val maxSize = properties.maxSizeFor(request.mediaType)
		if (request.contentType.lowercase() !in allowedTypes || request.fileSize > maxSize) {
			throw BusinessException(ErrorCode.INVALID_MEDIA_UPLOAD)
		}
	}
}
