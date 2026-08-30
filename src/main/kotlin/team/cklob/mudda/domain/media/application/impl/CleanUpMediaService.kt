package team.cklob.mudda.domain.media.application.impl

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.domain.media.infrastructure.MediaStorageProperties
import java.time.LocalDateTime

// Reclaims media that was uploaded and registered but never attached to a capsule -- an abandoned compose
// leaves the row and its S3 object behind indefinitely, which V4 made possible when it allowed a null
// time_capsule_id.
//
// ponytail: this only covers media that reached the database. An upload that was signed but never
// completed leaves an orphan under the pending/ prefix with no row to find it by; an S3 lifecycle rule on
// that prefix expires those without any application code, which is the right tool for it.
@Service
class CleanUpMediaService(
	private val mediaRepository: MediaRepository,
	private val mediaStorage: MediaStorage,
	private val properties: MediaStorageProperties,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Scheduled(cron = "\${media.cleanup.cron:0 0 4 * * *}")
	@Transactional
	fun execute(): Int {
		val threshold = LocalDateTime.now().minus(properties.pendingRetention)
		// Batched so a long-neglected backlog doesn't load every orphan into one transaction; the next run
		// picks up whatever is left.
		val orphans = mediaRepository.findUnattachedOlderThan(threshold, PageRequest.of(0, BATCH_SIZE))
		if (orphans.isEmpty()) return 0

		val deleted = orphans.filter { media ->
			// The row is only dropped once its object is gone. Deleting the row first on a storage failure
			// would strand the object with nothing left pointing at it.
			runCatching { mediaStorage.delete(media.s3Key) }
				.onFailure { logger.warn("failed to delete an orphaned media object: mediaId={}", media.id, it) }
				.isSuccess
		}
		mediaRepository.deleteAll(deleted)
		logger.info("cleaned up {} orphaned media rows (of {} candidates)", deleted.size, orphans.size)
		return deleted.size
	}

	private companion object {
		const val BATCH_SIZE = 500
	}
}
