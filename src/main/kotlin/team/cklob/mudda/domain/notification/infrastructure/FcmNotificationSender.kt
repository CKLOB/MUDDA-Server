package team.cklob.mudda.domain.notification.infrastructure

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification as FcmNotification
import org.slf4j.LoggerFactory
import team.cklob.mudda.domain.notification.application.NotificationSender

class FcmNotificationSender(private val messaging: FirebaseMessaging) : NotificationSender {
	private val logger = LoggerFactory.getLogger(javaClass)

	// FCM rejects a multicast carrying more than MAX_TOKENS_PER_REQUEST tokens outright, which would fail
	// the send for every device rather than just the excess. Nothing caps how many tokens one member
	// accumulates, so the list is chunked and each batch's permanently dead tokens are collected.
	override fun send(tokens: List<String>, title: String, body: String): List<String> =
		tokens.chunked(MAX_TOKENS_PER_REQUEST).flatMap { batch -> sendBatch(batch, title, body) }

	private fun sendBatch(tokens: List<String>, title: String, body: String): List<String> {
		val message = MulticastMessage.builder()
			.setNotification(FcmNotification.builder().setTitle(title).setBody(body).build())
			.addAllTokens(tokens)
			.build()
		val response = messaging.sendEachForMulticast(message)
		if (response.failureCount == 0) return emptyList()

		// Only tokens FCM reports as permanently dead are returned for pruning. A transient failure
		// (UNAVAILABLE, INTERNAL, quota) must keep its token: deleting it would silently unsubscribe a
		// working device from every future notification.
		return response.responses.withIndex().mapNotNull { (index, result) ->
			val errorCode = result.exception?.messagingErrorCode ?: return@mapNotNull null
			if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
				tokens[index]
			} else {
				logger.warn("fcm delivery failed but the token is kept: errorCode={}", errorCode)
				null
			}
		}
	}

	private companion object {
		const val MAX_TOKENS_PER_REQUEST = 500
	}
}
