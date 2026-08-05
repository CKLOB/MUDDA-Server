package team.cklob.mudda.domain.media.application

import team.cklob.mudda.domain.media.domain.type.MediaType
import java.util.UUID

data class MediaUploadKey(
	val memberId: Long,
	val mediaType: MediaType,
	val id: UUID,
) {
	val pendingKey: String = "pending/$memberId/${mediaType.name.lowercase()}/$id"
	val permanentKey: String = "media/$memberId/${mediaType.name.lowercase()}/$id"

	companion object {
		fun create(memberId: Long, mediaType: MediaType) = MediaUploadKey(memberId, mediaType, UUID.randomUUID())

		fun parse(key: String): MediaUploadKey? {
			val parts = key.split('/')
			if (parts.size != 4 || parts[0] != "pending") return null

			return runCatching {
				MediaUploadKey(parts[1].toLong(), MediaType.valueOf(parts[2].uppercase()), UUID.fromString(parts[3]))
			}.getOrNull()?.takeIf { it.pendingKey == key }
		}
	}
}
