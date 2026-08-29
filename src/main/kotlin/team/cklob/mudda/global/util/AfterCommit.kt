package team.cklob.mudda.global.util

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

// Defers a side effect that leaves the database -- an SSE broadcast, a push notification -- until the
// surrounding transaction commits. Firing it inline would announce work that a later rollback erases,
// leaving clients showing a capsule opening that never happened. With no active transaction the action
// runs immediately, so callers don't need to know which context they're in.
fun afterCommit(action: () -> Unit) {
	if (!TransactionSynchronizationManager.isSynchronizationActive()) {
		action()
		return
	}
	TransactionSynchronizationManager.registerSynchronization(
		object : TransactionSynchronization {
			override fun afterCommit() = action()
		},
	)
}
