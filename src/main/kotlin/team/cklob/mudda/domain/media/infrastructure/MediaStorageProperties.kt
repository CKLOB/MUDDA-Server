package team.cklob.mudda.domain.media.infrastructure

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("media.storage")
data class MediaStorageProperties(
	val bucket: String,
	val uploadUrlExpiration: Duration = Duration.ofMinutes(10),
	val accessUrlExpiration: Duration = Duration.ofMinutes(5),
	val maxImageSize: Long = 10 * 1024 * 1024,
	val maxVoiceSize: Long = 20 * 1024 * 1024,
	val maxVideoSize: Long = 100 * 1024 * 1024,
)
