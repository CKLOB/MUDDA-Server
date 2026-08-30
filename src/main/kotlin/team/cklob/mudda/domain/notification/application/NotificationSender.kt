package team.cklob.mudda.domain.notification.application

// Push transport port. The FCM adapter lives in infrastructure so tests (and any environment without
// Firebase credentials) can swap it for a no-op without touching the domain.
interface NotificationSender {
	// Returns the tokens FCM rejected as permanently invalid, so the caller can prune them.
	fun send(tokens: List<String>, title: String, body: String): List<String>
}
