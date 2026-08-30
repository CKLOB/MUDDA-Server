package team.cklob.mudda.domain.timecapsule.application.impl

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.timecapsule.domain.repository.KeyShareRepository
import java.time.LocalDateTime

// An expired capsule can never be opened again, so its key shares serve no purpose. Keeping them only
// leaves a wrapped share sitting in the database indefinitely, where an attacker who reaches the data can
// grind the lock secret offline for as long as they like. The encryption design calls for removing them
// on a schedule, and this is that job.
//
// Note this deletes only the shares, not the capsule row or its ciphertext: the capsule stays visible as
// an expired capsule, it simply becomes permanently unopenable, which is the intended end state.
@Service
class CleanUpKeyShareService(
	private val keyShareRepository: KeyShareRepository,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Scheduled(cron = "\${capsule.key-share-cleanup.cron:0 30 4 * * *}")
	@Transactional
	fun execute(): Int {
		val deleted = keyShareRepository.deleteSharesOfCapsulesExpiredBefore(LocalDateTime.now())
		if (deleted > 0) logger.info("deleted {} key shares belonging to expired capsules", deleted)
		return deleted
	}
}
