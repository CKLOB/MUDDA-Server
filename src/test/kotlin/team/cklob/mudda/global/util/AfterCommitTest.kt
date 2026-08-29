package team.cklob.mudda.global.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AfterCommitTest {
	@AfterEach fun clearTransaction() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization()
		}
	}

	@Test fun `with no transaction the action runs immediately`() {
		var ran = false

		afterCommit { ran = true }

		assertTrue(ran)
	}

	@Test fun `inside a transaction the action waits for commit`() {
		TransactionSynchronizationManager.initSynchronization()
		var ran = false

		afterCommit { ran = true }

		assertFalse(ran, "the side effect must not fire before commit")
		TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
		assertTrue(ran)
	}

	// A rollback never calls afterCommit, which is the whole point: no push, no broadcast for work that
	// was undone.
	@Test fun `a rolled back transaction never runs the action`() {
		TransactionSynchronizationManager.initSynchronization()
		var ran = false

		afterCommit { ran = true }
		TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCompletion(1) }

		assertFalse(ran)
		assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size)
	}
}
