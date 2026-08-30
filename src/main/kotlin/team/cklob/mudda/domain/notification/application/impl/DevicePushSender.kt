package team.cklob.mudda.domain.notification.application.impl

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.notification.application.NotificationSender
import team.cklob.mudda.domain.notification.domain.repository.DeviceTokenRepository

@Component
class DevicePushSender(
	private val deviceTokenRepository: DeviceTokenRepository,
	private val sender: NotificationSender,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	// Runs off the caller's thread in its own transaction: a slow or failing FCM round-trip must not
	// stretch -- or roll back -- the capsule/friend transaction that triggered it. The persisted
	// notification row is the source of truth, so a dropped push only costs the user a banner.
	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	fun send(memberId: Long, title: String, body: String) {
		val tokens = deviceTokenRepository.findTokensByMemberId(memberId)
		if (tokens.isEmpty()) return
		val invalid = try {
			sender.send(tokens, title, body)
		} catch (e: Exception) {
			logger.warn("push notification failed: memberId={}", memberId, e)
			return
		}
		if (invalid.isNotEmpty()) deviceTokenRepository.deleteByTokenIn(invalid)
	}
}
