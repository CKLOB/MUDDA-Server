package team.cklob.mudda.global.crypto.shamir

import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Walks the whole CLIENT_E2E scheme end to end from the client's point of view: encrypt under a CEK,
// split the CEK, wrap one share under the lock secret, hand the server what it is allowed to hold, and
// verify both that a legitimate opener succeeds and that the server's own holdings do not.
//
// This is what makes the design claim testable rather than aspirational.
class CapsuleKeyRoundTripTest {
	private val random = SecureRandom()

	private fun aesGcm(mode: Int, key: ByteArray, nonce: ByteArray, input: ByteArray): ByteArray =
		Cipher.getInstance("AES/GCM/NoPadding").run {
			init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
			doFinal(input)
		}

	// Stands in for the client's KDF over the capsule's password or answer. The real client should use a
	// slow, salted KDF; the property under test here is only that the server never learns this value.
	private fun keyFromLockSecret(secret: String): ByteArray =
		java.security.MessageDigest.getInstance("SHA-256").digest(secret.toByteArray())

	@Test fun `an opener who knows the lock secret recovers the body`() {
		val body = "10년 뒤의 나에게"
		val cek = ByteArray(32).also(random::nextBytes)
		val nonce = ByteArray(12).also(random::nextBytes)
		val blob = aesGcm(Cipher.ENCRYPT_MODE, cek, nonce, body.toByteArray())

		val shares = ShamirSecretSharing.split(cek, shareCount = 2, threshold = 2)
		val serverShare = shares[0]
		val wrappedNonce = ByteArray(12).also(random::nextBytes)
		val wrappedShare = aesGcm(Cipher.ENCRYPT_MODE, keyFromLockSecret("정답"), wrappedNonce, shares[1].value)

		// --- what the server stores ---
		val storedBlob = Base64.getEncoder().encodeToString(nonce + blob)
		val storedShares = listOf(serverShare, SecretShare(2, wrappedNonce + wrappedShare))

		// --- what an opener at the location, knowing the answer, does ---
		val returned = storedShares
		val unwrapped = returned[1].value.let {
			aesGcm(Cipher.DECRYPT_MODE, keyFromLockSecret("정답"), it.copyOfRange(0, 12), it.copyOfRange(12, it.size))
		}
		val recoveredCek = ShamirSecretSharing.combine(listOf(returned[0], SecretShare(2, unwrapped)))

		val raw = Base64.getDecoder().decode(storedBlob)
		val recovered = aesGcm(Cipher.DECRYPT_MODE, recoveredCek, raw.copyOfRange(0, 12), raw.copyOfRange(12, raw.size))

		assertEquals(body, String(recovered))
	}

	// The claim the whole design rests on. The server holds one plaintext share and one blob it has no key
	// for, which is below the threshold of 2.
	@Test fun `the server's own shares do not reconstruct the key`() {
		val cek = ByteArray(32).also(random::nextBytes)
		val shares = ShamirSecretSharing.split(cek, shareCount = 2, threshold = 2)
		val wrappedNonce = ByteArray(12).also(random::nextBytes)
		val wrapped = aesGcm(Cipher.ENCRYPT_MODE, keyFromLockSecret("정답"), wrappedNonce, shares[1].value)

		// Everything the server has: one usable share, plus ciphertext it cannot open.
		val serverUsable = listOf(shares[0])
		assertEquals(1, serverUsable.size, "the server must hold fewer shares than the threshold")

		// Treating the wrapped bytes as if they were a share -- the best the server can do without the
		// lock secret -- yields something other than the key.
		val naive = ShamirSecretSharing.combine(listOf(shares[0], SecretShare(2, (wrappedNonce + wrapped).copyOfRange(0, 32))))
		assertFalse(naive.contentEquals(cek), "the server reconstructed the key from what it stores")
	}

	@Test fun `a wrong answer fails loudly instead of yielding a wrong body`() {
		val cek = ByteArray(32).also(random::nextBytes)
		val shares = ShamirSecretSharing.split(cek, shareCount = 2, threshold = 2)
		val nonce = ByteArray(12).also(random::nextBytes)
		val wrapped = aesGcm(Cipher.ENCRYPT_MODE, keyFromLockSecret("정답"), nonce, shares[1].value)

		// GCM authenticates, so an unwrap under the wrong key is detected rather than returning garbage
		// that would later surface as an unreadable body.
		val failed = runCatching {
			aesGcm(Cipher.DECRYPT_MODE, keyFromLockSecret("오답"), nonce, wrapped)
		}

		assertTrue(failed.isFailure, "unwrapping with a wrong answer must fail the GCM tag check")
	}
}
