package team.cklob.mudda.domain.feed.infrastructure

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager
import team.cklob.mudda.domain.feed.domain.type.FeedType
import team.cklob.mudda.domain.feed.presentation.response.FeedMemberResponse
import team.cklob.mudda.domain.feed.presentation.response.FeedResponse
import java.time.LocalDateTime
import kotlin.test.assertEquals

// The emitter lifecycle callbacks (onCompletion/onTimeout/onError) only fire once Spring MVC has wired a
// handler to the emitter, so eviction-on-disconnect is not observable from a plain unit test. What is
// testable here -- and what actually carries risk -- is the registration bookkeeping and the after-commit
// deferral that keeps a rolled-back capsule open from being announced.
class FeedBroadcasterTest {
	private val broadcaster = FeedBroadcaster()

	private val feed = FeedResponse(
		feedId = 1, type = FeedType.CAPSULE_OPENED, message = "m",
		member = FeedMemberResponse(1, "nick", null), capsuleId = 2, createdAt = LocalDateTime.now(),
	)

	@AfterEach fun clearTransaction() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization()
		}
	}

	@Test fun `each subscriber is registered`() {
		broadcaster.subscribe(1000)
		broadcaster.subscribe(1000)

		assertEquals(2, broadcaster.subscriberCount())
	}

	@Test fun `broadcasting inside a transaction is deferred until commit`() {
		TransactionSynchronizationManager.initSynchronization()

		broadcaster.broadcast(feed)

		assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size)
	}

	@Test fun `broadcasting outside a transaction registers nothing to defer`() {
		broadcaster.broadcast(feed)

		assertEquals(false, TransactionSynchronizationManager.isSynchronizationActive())
	}
}
