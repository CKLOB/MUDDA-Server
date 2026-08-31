package team.cklob.mudda.domain.timecapsule.application

import org.springframework.stereotype.Component
import java.util.Base64
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleEncryptionMode
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.presentation.request.CreateCapsuleRequest
import team.cklob.mudda.domain.timecapsule.presentation.request.KeyShareRequest
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

// Decides which encryption mode a capsule gets and refuses payloads that would quietly break the
// guarantee the mode advertises. Kept out of CreateCapsuleService so the whole rule set reads in one
// place -- this is the boundary CLAUDE.md asks to keep explicit rather than scattered.
@Component
class CapsuleEncryptionPolicy {
	fun resolveMode(lockType: CapsuleLockType): CapsuleEncryptionMode = when (lockType) {
		// An unlocked capsule opens on location alone, and the server stores the location. Any secret it
		// withheld from itself it could re-derive, so end-to-end encryption is not achievable here and
		// claiming it would be worse than not offering it.
		CapsuleLockType.NONE -> CapsuleEncryptionMode.SERVER_ENVELOPE
		CapsuleLockType.PASSWORD, CapsuleLockType.QUESTION -> CapsuleEncryptionMode.CLIENT_E2E
	}

	fun validate(request: CreateCapsuleRequest, mode: CapsuleEncryptionMode) {
		validateQuestionText(request)
		when (mode) {
			CapsuleEncryptionMode.SERVER_ENVELOPE -> validateServerEnvelope(request)
			CapsuleEncryptionMode.CLIENT_E2E -> validateClientE2e(request)
		}
	}

	// The question is the only lock field the server still receives -- it is prompt text shown to the
	// opener, never the answer.
	private fun validateQuestionText(request: CreateCapsuleRequest) {
		val valid = when (request.lockType) {
			CapsuleLockType.QUESTION -> !request.question.isNullOrBlank()
			CapsuleLockType.NONE, CapsuleLockType.PASSWORD -> request.question == null
		}
		if (!valid) throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION)
	}

	private fun validateServerEnvelope(request: CreateCapsuleRequest) {
		if (request.content.isNullOrBlank()) throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION)
		// Rejected rather than ignored: silently dropping key material would leave the client believing it
		// created an end-to-end capsule.
		if (request.contentCipher != null || !request.keyShares.isNullOrEmpty() || request.keyThreshold != null) {
			throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION)
		}
	}

	private fun validateClientE2e(request: CreateCapsuleRequest) {
		// The server must never receive a plaintext body for a capsule it is not supposed to be able to read.
		if (request.content != null) throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION)
		if (request.contentCipher.isNullOrBlank()) throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION)

		val shares = request.keyShares
		val threshold = request.keyThreshold
		if (shares.isNullOrEmpty() || threshold == null) throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION)
		if (shares.map { it.index }.toSet().size != shares.size) throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION)

		// The guarantee this mode advertises is that the server cannot reconstruct the key on its own. A
		// wrapped share does not count: it is ciphertext under a key derived from the lock secret, which the
		// server only ever sees as a bcrypt hash. Enforced here rather than trusted to the client, because a
		// client bug that sent every share in the clear would silently downgrade the capsule to plaintext
		// while still reporting it as end-to-end.
		val usableByServer = shares.count { it.isWrapped == false }
		if (usableByServer >= threshold) throw BusinessException(ErrorCode.SERVER_HOLDS_KEY_QUORUM)

		// Symmetrically, at least one wrapped share has to be present: otherwise the client has no way to
		// reach the threshold either and the capsule would be unopenable by anyone.
		if (shares.none { it.isWrapped == true }) throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION)

		validateWrappedShapes(shares)
	}

	// `isWrapped` is a client assertion, and the server fundamentally cannot verify it: well-encrypted
	// bytes are indistinguishable from random ones, so no check can prove a share is really wrapped. What
	// is checkable is its shape. A share wrapped with AES-256-GCM as the protocol specifies carries a
	// 12-byte nonce and a 16-byte tag on top of the plaintext share, so a wrapped share must be exactly
	// AEAD_OVERHEAD_BYTES longer than a plaintext one.
	//
	// This catches the realistic failure -- a client bug that labels raw shares as wrapped and silently
	// downgrades the capsule to server-readable. It does not stop a client that deliberately pads raw
	// bytes to the right length. That residual case is accepted: a client lying here only exposes its own
	// capsule, whose secret it already holds, so the guarantee is precisely "the server cannot read a
	// capsule whose owner followed the protocol".
	private fun validateWrappedShapes(shares: List<KeyShareRequest>) {
		val decoded = shares.map {
			it to runCatching { Base64.getDecoder().decode(it.data) }
				.getOrElse { throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION) }
		}
		// With no plaintext share there is no baseline length to compare against, and the server holding
		// zero usable shares is already the strongest case, so there is nothing left to check.
		val plainLength = decoded.firstOrNull { (share, _) -> share.isWrapped == false }?.second?.size ?: return

		decoded.forEach { (share, bytes) ->
			val expected = if (share.isWrapped == true) plainLength + AEAD_OVERHEAD_BYTES else plainLength
			if (bytes.size != expected) throw BusinessException(ErrorCode.INVALID_CAPSULE_ENCRYPTION)
		}
	}

	private companion object {
		// AES-GCM with a 96-bit nonce and a 128-bit tag, as the encryption design specifies.
		const val AEAD_OVERHEAD_BYTES = 12 + 16
	}
}
