package team.cklob.mudda.domain.feed.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import team.cklob.mudda.domain.feed.presentation.response.FeedResponse
import team.cklob.mudda.global.util.afterCommit
import java.util.concurrent.CopyOnWriteArrayList

// ponytail: in-process fan-out. The discovery feed is the same stream for everyone, so subscribers are a
// flat list with no per-member routing. This only reaches subscribers connected to *this* instance --
// move to Redis pub/sub (Redis is already a dependency) when the app runs behind more than one container.
@Component
class FeedBroadcaster {
	private val logger = LoggerFactory.getLogger(javaClass)
	private val emitters = CopyOnWriteArrayList<SseEmitter>()

	fun subscribe(timeoutMillis: Long): SseEmitter {
		val emitter = SseEmitter(timeoutMillis)
		emitters += emitter
		// Every terminal state has to remove the emitter, or the list grows without bound as clients come
		// and go and broadcast() ends up walking mostly-dead entries.
		emitter.onCompletion { emitters -= emitter }
		emitter.onTimeout { emitters -= emitter }
		emitter.onError { emitters -= emitter }
		// An initial comment event makes the proxy flush response headers, so the client's EventSource
		// fires `onopen` immediately instead of hanging until the first real feed item.
		runCatching { emitter.send(SseEmitter.event().comment("connected")) }
			.onFailure { emitters -= emitter }
		return emitter
	}

	fun broadcast(feed: FeedResponse) = afterCommit {
		emitters.forEach { emitter ->
			try {
				emitter.send(SseEmitter.event().name("feed").data(feed))
			} catch (e: Exception) {
				// A send failure means the client is gone (or the connection broke). Drop it here rather
				// than waiting for the timeout callback; completeWithError triggers onError, which is a
				// no-op removal once the entry is already out of the list.
				logger.debug("dropping a dead feed subscriber", e)
				emitters -= emitter
				runCatching { emitter.completeWithError(e) }
			}
		}
	}

	// Exposed for tests and for an eventual health/metrics readout.
	fun subscriberCount(): Int = emitters.size
}
