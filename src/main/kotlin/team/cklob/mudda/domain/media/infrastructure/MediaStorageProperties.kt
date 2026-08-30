package team.cklob.mudda.domain.media.infrastructure

import org.springframework.boot.context.properties.ConfigurationProperties
import team.cklob.mudda.domain.media.domain.type.MediaType
import java.time.Duration

@ConfigurationProperties("media.storage")
data class MediaStorageProperties(
	val bucket: String,
	val uploadUrlExpiration: Duration = Duration.ofMinutes(10),
	val accessUrlExpiration: Duration = Duration.ofMinutes(5),
	val maxImageSize: Long = 10 * 1024 * 1024,
	val maxVoiceSize: Long = 20 * 1024 * 1024,
	val maxVideoSize: Long = 100 * 1024 * 1024,
	// How long a registered-but-unattached media row is kept before the cleanup job reclaims it. Must stay
	// comfortably longer than the time a user might spend composing a capsule after picking their photos.
	val pendingRetention: Duration = Duration.ofDays(1),
) {
	fun maxSizeFor(mediaType: MediaType) = when (mediaType) {
		MediaType.IMAGE -> maxImageSize
		MediaType.VIDEO -> maxVideoSize
		MediaType.VOICE -> maxVoiceSize
	}

	fun allowedContentTypesFor(mediaType: MediaType) = ALLOWED_CONTENT_TYPES[mediaType].orEmpty()

	companion object {
		private val ALLOWED_CONTENT_TYPES = mapOf(
			MediaType.IMAGE to setOf("image/jpeg", "image/png", "image/webp"),
			MediaType.VIDEO to setOf("video/mp4", "video/quicktime"),
			MediaType.VOICE to setOf("audio/mpeg", "audio/mp4", "audio/wav"),
		)
	}
}
