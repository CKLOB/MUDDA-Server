package team.cklob.mudda.domain.timecapsule.application

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleEncryptionMode
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.domain.timecapsule.presentation.request.CreateCapsuleRequest
import team.cklob.mudda.domain.timecapsule.presentation.request.KeyShareRequest
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import kotlin.test.assertEquals

// These rules are the whole guarantee behind CLIENT_E2E. If the policy lets a bad payload through, the
// capsule still reports itself as end-to-end while the server can actually read it, which is worse than
// not offering the mode at all.
class CapsuleEncryptionPolicyTest {
	private val policy = CapsuleEncryptionPolicy()

	private fun request(
		lockType: CapsuleLockType,
		content: String? = null,
		contentCipher: String? = null,
		keyShares: List<KeyShareRequest>? = null,
		keyThreshold: Int? = null,
	) = CreateCapsuleRequest(
		name = "capsule", content = content, contentCipher = contentCipher, keyShares = keyShares,
		keyThreshold = keyThreshold, latitude = 37.5, longitude = 127.0,
		openAt = LocalDateTime.now().plusDays(1), visibility = CapsuleVisibility.PUBLIC, lockType = lockType,
		password = if (lockType == CapsuleLockType.PASSWORD) "pw" else null,
		question = if (lockType == CapsuleLockType.QUESTION) "q" else null,
		answer = if (lockType == CapsuleLockType.QUESTION) "a" else null,
	)

	private fun share(index: Int, wrapped: Boolean) = KeyShareRequest(index, "c2hhcmU=", wrapped)

	// -------- mode resolution --------

	@Test fun `an unlocked capsule cannot be end-to-end encrypted`() {
		// Not a limitation of the implementation: the only unlock condition is the location, which the
		// server stores, so it could always satisfy the condition itself.
		assertEquals(CapsuleEncryptionMode.SERVER_ENVELOPE, policy.resolveMode(CapsuleLockType.NONE))
	}

	@Test fun `locked capsules are end-to-end encrypted`() {
		assertEquals(CapsuleEncryptionMode.CLIENT_E2E, policy.resolveMode(CapsuleLockType.PASSWORD))
		assertEquals(CapsuleEncryptionMode.CLIENT_E2E, policy.resolveMode(CapsuleLockType.QUESTION))
	}

	// -------- the guarantee --------

	@Test fun `a payload giving the server a full quorum of plaintext shares is rejected`() {
		val request = request(
			CapsuleLockType.PASSWORD, contentCipher = "blob",
			keyShares = listOf(share(1, wrapped = false), share(2, wrapped = false), share(3, wrapped = true)),
			keyThreshold = 2,
		)

		val error = assertThrows<BusinessException> { policy.validate(request, CapsuleEncryptionMode.CLIENT_E2E) }

		assertEquals(ErrorCode.SERVER_HOLDS_KEY_QUORUM, error.errorCode)
	}

	@Test fun `a wrapped share does not count toward the server's quorum`() {
		// One plaintext share plus one wrapped share at threshold 2: the server holds only one usable share.
		val request = request(
			CapsuleLockType.PASSWORD, contentCipher = "blob",
			keyShares = listOf(share(1, wrapped = false), share(2, wrapped = true)),
			keyThreshold = 2,
		)

		policy.validate(request, CapsuleEncryptionMode.CLIENT_E2E)
	}

	@Test fun `a payload with no wrapped share is rejected because nobody could open it`() {
		val request = request(
			CapsuleLockType.PASSWORD, contentCipher = "blob",
			keyShares = listOf(share(1, wrapped = false)), keyThreshold = 2,
		)

		val error = assertThrows<BusinessException> { policy.validate(request, CapsuleEncryptionMode.CLIENT_E2E) }

		assertEquals(ErrorCode.INVALID_CAPSULE_ENCRYPTION, error.errorCode)
	}

	@Test fun `an end-to-end capsule may not send plaintext content`() {
		val request = request(
			CapsuleLockType.PASSWORD, content = "plaintext", contentCipher = "blob",
			keyShares = listOf(share(1, wrapped = true)), keyThreshold = 2,
		)

		val error = assertThrows<BusinessException> { policy.validate(request, CapsuleEncryptionMode.CLIENT_E2E) }

		assertEquals(ErrorCode.INVALID_CAPSULE_ENCRYPTION, error.errorCode)
	}

	@Test fun `an end-to-end capsule without a cipher blob is rejected`() {
		val request = request(CapsuleLockType.PASSWORD, keyShares = listOf(share(1, wrapped = true)), keyThreshold = 2)

		assertThrows<BusinessException> { policy.validate(request, CapsuleEncryptionMode.CLIENT_E2E) }
	}

	@Test fun `duplicate share indices are rejected`() {
		val request = request(
			CapsuleLockType.PASSWORD, contentCipher = "blob",
			keyShares = listOf(share(1, wrapped = false), share(1, wrapped = true)), keyThreshold = 2,
		)

		assertThrows<BusinessException> { policy.validate(request, CapsuleEncryptionMode.CLIENT_E2E) }
	}

	// -------- server envelope --------

	@Test fun `an unlocked capsule requires plaintext content`() {
		val error = assertThrows<BusinessException> {
			policy.validate(request(CapsuleLockType.NONE), CapsuleEncryptionMode.SERVER_ENVELOPE)
		}

		assertEquals(ErrorCode.INVALID_CAPSULE_ENCRYPTION, error.errorCode)
	}

	// Silently dropping the key material would leave the client thinking it made an end-to-end capsule.
	@Test fun `an unlocked capsule carrying key material is rejected rather than ignored`() {
		val request = request(
			CapsuleLockType.NONE, content = "plaintext",
			keyShares = listOf(share(1, wrapped = true)), keyThreshold = 2,
		)

		val error = assertThrows<BusinessException> { policy.validate(request, CapsuleEncryptionMode.SERVER_ENVELOPE) }

		assertEquals(ErrorCode.INVALID_CAPSULE_ENCRYPTION, error.errorCode)
	}

	@Test fun `a plain unlocked capsule passes`() {
		policy.validate(request(CapsuleLockType.NONE, content = "plaintext"), CapsuleEncryptionMode.SERVER_ENVELOPE)
	}
}
