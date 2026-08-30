package team.cklob.mudda.domain.notification.infrastructure

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.SendResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

// FCM rejects a multicast carrying more than 500 tokens, which would fail delivery for every device
// rather than just the excess.
class FcmNotificationSenderTest {
	private val messaging = mockk<FirebaseMessaging>()
	private val sender = FcmNotificationSender(messaging)

	private fun allDelivered() = mockk<BatchResponse> {
		every { failureCount } returns 0
		every { responses } returns emptyList<SendResponse>()
	}

	@Test fun `more than 500 tokens are split across requests`() {
		val sent = mutableListOf<MulticastMessage>()
		val captured = slot<MulticastMessage>()
		every { messaging.sendEachForMulticast(capture(captured)) } answers {
			sent += captured.captured
			allDelivered()
		}

		sender.send((1..1200).map { "token-$it" }, "title", "body")

		assertEquals(3, sent.size)
	}

	@Test fun `a full batch still takes a single request`() {
		var calls = 0
		every { messaging.sendEachForMulticast(any()) } answers { calls++; allDelivered() }

		sender.send((1..500).map { "token-$it" }, "title", "body")

		assertEquals(1, calls)
	}

	@Test fun `permanently dead tokens from every batch are collected`() {
		val dead = mockk<SendResponse> {
			every { exception } returns mockk { every { messagingErrorCode } returns com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED }
		}
		every { messaging.sendEachForMulticast(any()) } returns mockk<BatchResponse> {
			every { failureCount } returns 1
			every { responses } returns listOf(dead)
		}

		// Two batches, each reporting its first token dead.
		val invalid = sender.send((1..600).map { "token-$it" }, "title", "body")

		assertEquals(listOf("token-1", "token-501"), invalid)
	}
}
