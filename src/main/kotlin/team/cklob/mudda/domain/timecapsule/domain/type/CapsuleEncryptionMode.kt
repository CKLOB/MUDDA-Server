package team.cklob.mudda.domain.timecapsule.domain.type

// Which side holds the key to a capsule's body.
enum class CapsuleEncryptionMode {
	// The server encrypts and decrypts the body with its own master key, so it can read the plaintext.
	// Used for lockType = NONE, where end-to-end encryption is impossible by construction: the only
	// unlock condition is being at the capsule's coordinates, and the server stores those coordinates,
	// so any secret it could withhold from itself it could also re-derive.
	SERVER_ENVELOPE,

	// The client encrypts the body under a key the server never sees. The server keeps an opaque blob plus
	// fewer key shares than the reconstruction threshold, so it cannot recover the body on its own.
	// Requires a lock (PASSWORD or QUESTION): the lock secret is the one input the server does not hold.
	CLIENT_E2E,
}
