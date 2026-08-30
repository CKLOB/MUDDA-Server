package team.cklob.mudda.domain.timecapsule.application.impl

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CapsuleNotificationTextTest {
	// A capsule name is accepted up to 255 characters. Left whole, the assembled sentence would be cut at
	// the notification column width and lose its trailing clause.
	@Test fun `a long capsule name is shortened so the sentence survives`() {
		val message = "nick님이 '${"가".repeat(255).shortenForNotification()}'을(를) 남겼어요."

		assertTrue(message.length <= 255)
		assertTrue(message.endsWith("'을(를) 남겼어요."))
	}

	@Test fun `a short capsule name is left alone`() {
		assertEquals("첫 캡슐", "첫 캡슐".shortenForNotification())
	}
}
