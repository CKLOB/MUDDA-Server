package team.cklob.mudda.domain.media.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.application.SignedUrl
import team.cklob.mudda.domain.media.application.StoredObject
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime

@Component
class S3MediaStorage(
	private val s3Client: S3Client,
	private val s3Presigner: S3Presigner,
	private val properties: MediaStorageProperties,
) : MediaStorage {
	private val logger = LoggerFactory.getLogger(javaClass)

	override fun createUploadUrl(key: String, contentType: String, contentLength: Long): SignedUrl = storageCall {
		val request = PutObjectRequest.builder()
			.bucket(properties.bucket)
			.key(key)
			.contentType(contentType)
			.contentLength(contentLength)
			.build()
		val presigned = s3Presigner.presignPutObject(
			PutObjectPresignRequest.builder()
				.signatureDuration(properties.uploadUrlExpiration)
				.putObjectRequest(request)
				.build(),
		)
		SignedUrl(presigned.url().toString(), LocalDateTime.now().plus(properties.uploadUrlExpiration))
	}

	override fun inspect(key: String): StoredObject = storageCall {
		val response = s3Client.headObject(HeadObjectRequest.builder().bucket(properties.bucket).key(key).build())
		StoredObject(response.contentType(), response.contentLength())
	}

	override fun copy(sourceKey: String, destinationKey: String) = storageCall {
		s3Client.copyObject(
			CopyObjectRequest.builder()
				.copySource("${properties.bucket}/$sourceKey")
				.destinationBucket(properties.bucket)
				.destinationKey(destinationKey)
				.build(),
		)
		Unit
	}

	override fun createAccessUrl(key: String): SignedUrl = storageCall {
		val request = GetObjectRequest.builder().bucket(properties.bucket).key(key).build()
		val presigned = s3Presigner.presignGetObject(
			GetObjectPresignRequest.builder()
				.signatureDuration(properties.accessUrlExpiration)
				.getObjectRequest(request)
				.build(),
		)
		SignedUrl(presigned.url().toString(), LocalDateTime.now().plus(properties.accessUrlExpiration))
	}

	override fun delete(key: String) = storageCall {
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket).key(key).build())
		Unit
	}

	private fun <T> storageCall(block: () -> T): T = try {
		block()
	} catch (exception: BusinessException) {
		throw exception
	} catch (exception: Exception) {
		logger.error("Media storage request failed", exception)
		throw BusinessException(ErrorCode.MEDIA_STORAGE_ERROR)
	}
}
