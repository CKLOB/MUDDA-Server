package team.cklob.mudda.global.crypto.shamir

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.SecureRandom
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// A bug here is unrecoverable in production: a capsule whose key cannot be reassembled is sealed forever,
// with no server-side copy to fall back on. The properties below are asserted rather than sampled where
// the search space allows it.
class ShamirSecretSharingTest {
	private val random = SecureRandom()

	private fun secret(size: Int = 32) = ByteArray(size).also(random::nextBytes)

	@Test fun `any subset of exactly the threshold size recovers the secret`() {
		val secret = secret()
		val shares = ShamirSecretSharing.split(secret, shareCount = 5, threshold = 3)

		// All 10 three-element subsets must work, not just the first three shares.
		for (i in 0 until 5) {
			for (j in i + 1 until 5) {
				for (k in j + 1 until 5) {
					val subset = listOf(shares[i], shares[j], shares[k])
					assertContentEquals(secret, ShamirSecretSharing.combine(subset), "subset $i,$j,$k failed")
				}
			}
		}
	}

	@Test fun `more shares than the threshold still recover the secret`() {
		val secret = secret()
		val shares = ShamirSecretSharing.split(secret, shareCount = 5, threshold = 3)

		assertContentEquals(secret, ShamirSecretSharing.combine(shares))
	}

	// The security property, not merely a difficulty property: with threshold-1 shares every candidate
	// secret is still equally likely. Combining a short subset must therefore not return the secret.
	@Test fun `threshold minus one shares do not reveal the secret`() {
		val secret = secret()
		val shares = ShamirSecretSharing.split(secret, shareCount = 5, threshold = 3)

		repeat(200) {
			val subset = shares.shuffled().take(2)
			assertFalse(
				ShamirSecretSharing.combine(subset).contentEquals(secret),
				"two of three shares reconstructed the secret",
			)
		}
	}

	// Sharpens the previous test: for a one-byte secret with a 2-of-n split, a single share is consistent
	// with every one of the 256 possible secrets. Pairing it with each possible second share must produce
	// all 256 values exactly once -- the signature of information-theoretic secrecy.
	@Test fun `one share of a two-of-n split is consistent with every possible secret`() {
		val shares = ShamirSecretSharing.split(byteArrayOf(0x42), shareCount = 2, threshold = 2)
		val known = shares.first()

		val reachable = (0..255).map { candidate ->
			ShamirSecretSharing.combine(listOf(known, SecretShare(2, byteArrayOf(candidate.toByte()))))[0].toInt() and 0xFF
		}

		assertEquals(256, reachable.toSet().size, "a single share narrowed the secret down")
	}

	@Test fun `the documented two-of-three and three-of-five configurations round-trip`() {
		for ((shareCount, threshold) in listOf(3 to 2, 5 to 3)) {
			val secret = secret()
			val shares = ShamirSecretSharing.split(secret, shareCount, threshold)

			assertEquals(shareCount, shares.size)
			assertContentEquals(secret, ShamirSecretSharing.combine(shares.take(threshold)))
		}
	}

	@Test fun `secrets containing zero and full bytes survive the round trip`() {
		// 0x00 and 0xFF exercise the branches where the log table is not defined and where it saturates.
		val secret = byteArrayOf(0, 0, 0, -1, -1, 0, 127, -128)
		val shares = ShamirSecretSharing.split(secret, shareCount = 4, threshold = 2)

		assertContentEquals(secret, ShamirSecretSharing.combine(shares.take(2)))
	}

	@Test fun `a 256 bit key round-trips at the maximum share count`() {
		val secret = secret(32)
		val shares = ShamirSecretSharing.split(secret, shareCount = MAX_SHARES, threshold = 2)

		assertEquals(MAX_SHARES, shares.size)
		assertContentEquals(secret, ShamirSecretSharing.combine(listOf(shares.first(), shares.last())))
	}

	@Test fun `splitting is randomised so two splits of one secret differ`() {
		val secret = secret()

		val first = ShamirSecretSharing.split(secret, shareCount = 3, threshold = 2)
		val second = ShamirSecretSharing.split(secret, shareCount = 3, threshold = 2)

		assertFalse(first[0].value.contentEquals(second[0].value), "share bytes repeated across splits")
		assertContentEquals(secret, ShamirSecretSharing.combine(second.take(2)))
	}

	@Test fun `share indices start at one so no share sits on the secret itself`() {
		val shares = ShamirSecretSharing.split(secret(), shareCount = 3, threshold = 2)

		assertEquals(listOf(1, 2, 3), shares.map { it.index })
		assertTrue(shares.none { it.index == 0 }, "x=0 is where the secret lives and must never be a share")
	}

	@Test fun `a threshold below two is rejected because it would store the secret in the clear`() {
		assertThrows<IllegalArgumentException> { ShamirSecretSharing.split(secret(), shareCount = 3, threshold = 1) }
	}

	@Test fun `a share count below the threshold is rejected as unrecoverable`() {
		assertThrows<IllegalArgumentException> { ShamirSecretSharing.split(secret(), shareCount = 2, threshold = 3) }
	}

	@Test fun `an empty secret is rejected`() {
		assertThrows<IllegalArgumentException> { ShamirSecretSharing.split(ByteArray(0), shareCount = 3, threshold = 2) }
	}

	@Test fun `duplicate share indices are rejected instead of returning garbage`() {
		val shares = ShamirSecretSharing.split(secret(), shareCount = 3, threshold = 2)
		val duplicated = listOf(shares[0], shares[0])

		assertThrows<IllegalArgumentException> { ShamirSecretSharing.combine(duplicated) }
	}

	@Test fun `shares of differing lengths are rejected`() {
		val shares = ShamirSecretSharing.split(secret(), shareCount = 3, threshold = 2)
		val mismatched = listOf(shares[0], SecretShare(2, ByteArray(4)))

		assertThrows<IllegalArgumentException> { ShamirSecretSharing.combine(mismatched) }
	}

	@Test fun `share equality compares bytes rather than references`() {
		val first = SecretShare(1, byteArrayOf(1, 2, 3))
		val second = SecretShare(1, byteArrayOf(1, 2, 3))

		assertEquals(first, second)
		assertEquals(first.hashCode(), second.hashCode())
	}

	@Test fun `a share never prints its bytes`() {
		val share = SecretShare(1, byteArrayOf(9, 9, 9))

		assertFalse(share.toString().contains("9"), "share bytes leaked through toString")
	}
}
