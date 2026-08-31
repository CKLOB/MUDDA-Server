package team.cklob.mudda.global.crypto.shamir

import java.security.SecureRandom

// One share of a split secret. `index` is the x-coordinate the polynomials were evaluated at and must be
// kept alongside the bytes -- Lagrange interpolation cannot recover the secret without knowing which x
// each share belongs to.
data class SecretShare(val index: Int, val value: ByteArray) {
	init {
		require(index in 1..MAX_SHARES) { "A share index must be in 1..$MAX_SHARES." }
		require(value.isNotEmpty()) { "A share must carry at least one byte." }
	}

	// data class equality on a ByteArray would compare references, which silently breaks set membership
	// and assertion messages.
	override fun equals(other: Any?): Boolean =
		this === other || (other is SecretShare && index == other.index && value.contentEquals(other.value))

	override fun hashCode(): Int = 31 * index + value.contentHashCode()

	// Never let share bytes reach a log or an assertion message.
	override fun toString(): String = "SecretShare(index=$index, value=***)"
}

const val MAX_SHARES = 255

// Shamir's Secret Sharing over GF(256).
//
// The secret is split byte-wise: for each byte a random polynomial of degree threshold-1 is chosen with
// the secret byte as its constant term, then evaluated at x = 1..shareCount. Recovering the constant term
// needs `threshold` points; with any fewer, every possible constant term remains equally likely, so
// threshold-1 shares leak nothing about the secret rather than merely making it hard to guess.
object ShamirSecretSharing {
	private val random = SecureRandom()

	fun split(secret: ByteArray, shareCount: Int, threshold: Int): List<SecretShare> {
		require(secret.isNotEmpty()) { "Cannot split an empty secret." }
		require(threshold >= 2) { "A threshold below 2 would store the secret in the clear." }
		require(shareCount in threshold..MAX_SHARES) {
			"shareCount must be between the threshold and $MAX_SHARES, otherwise the secret is unrecoverable."
		}

		val shares = Array(shareCount) { ByteArray(secret.size) }
		val coefficients = IntArray(threshold)

		secret.forEachIndexed { byteIndex, secretByte ->
			// A fresh polynomial per byte. Reusing one across bytes would leak relationships between them.
			coefficients[0] = secretByte.toInt() and 0xFF
			for (degree in 1 until threshold) {
				coefficients[degree] = random.nextInt(256)
			}
			for (shareIndex in 0 until shareCount) {
				val x = shareIndex + 1
				shares[shareIndex][byteIndex] = evaluate(coefficients, x).toByte()
			}
		}

		return shares.mapIndexed { shareIndex, value -> SecretShare(shareIndex + 1, value) }
	}

	fun combine(shares: List<SecretShare>): ByteArray {
		require(shares.isNotEmpty()) { "Cannot combine an empty share list." }
		val indices = shares.map { it.index }
		require(indices.toSet().size == indices.size) {
			"Duplicate share indices cannot be interpolated; they describe the same point."
		}
		val length = shares.first().value.size
		require(shares.all { it.value.size == length }) { "Shares of differing lengths did not come from one secret." }

		val secret = ByteArray(length)
		for (byteIndex in 0 until length) {
			val points = shares.map { it.index to (it.value[byteIndex].toInt() and 0xFF) }
			secret[byteIndex] = interpolateAtZero(points).toByte()
		}
		return secret
	}

	// Horner's method, so evaluation stays a single pass over the coefficients.
	private fun evaluate(coefficients: IntArray, x: Int): Int {
		var result = 0
		for (degree in coefficients.indices.reversed()) {
			result = GaloisField256.add(GaloisField256.multiply(result, x), coefficients[degree])
		}
		return result
	}

	// Lagrange interpolation evaluated at x = 0, which is where the secret sits as the constant term.
	private fun interpolateAtZero(points: List<Pair<Int, Int>>): Int {
		var result = 0
		points.forEachIndexed { i, (xi, yi) ->
			var basis = 1
			points.forEachIndexed { j, (xj, _) ->
				if (i != j) {
					// (0 - xj) / (xi - xj); subtraction is XOR, so 0 - xj is simply xj.
					basis = GaloisField256.multiply(basis, GaloisField256.divide(xj, GaloisField256.subtract(xi, xj)))
				}
			}
			result = GaloisField256.add(result, GaloisField256.multiply(yi, basis))
		}
		return result
	}
}
