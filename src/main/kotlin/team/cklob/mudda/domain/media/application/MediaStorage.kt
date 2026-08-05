package team.cklob.mudda.domain.media.application

import java.time.LocalDateTime

interface MediaStorage {
	fun createUploadUrl(key: String, contentType: String, contentLength: Long): SignedUrl
	fun inspect(key: String): StoredObject
	fun copy(sourceKey: String, destinationKey: String)
	fun createAccessUrl(key: String): SignedUrl
	fun delete(key: String)
}

data class SignedUrl(
	val url: String,
	val expiresAt: LocalDateTime,
)

data class StoredObject(
	val contentType: String?,
	val contentLength: Long,
)
