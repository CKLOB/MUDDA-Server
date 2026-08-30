package team.cklob.mudda.global.crypto.shamir

// Arithmetic in GF(2^8) using the AES irreducible polynomial x^8 + x^4 + x^3 + x + 1 (0x11B).
//
// Every byte value is an element of the field, so a secret can be split byte-by-byte with no encoding
// or padding, and addition is XOR. Multiplication and division go through log/exp tables built from the
// generator 0x03: a table lookup is both faster and free of the data-dependent branching that a
// shift-and-xor loop would introduce.
internal object GaloisField256 {
	private const val FIELD_SIZE = 256
	private const val IRREDUCIBLE = 0x11B
	private const val GENERATOR = 0x03

	// exp is doubled in length so that a + b (which can reach 508) never needs a modulo before lookup.
	private val exp = IntArray(FIELD_SIZE * 2)
	private val log = IntArray(FIELD_SIZE)

	init {
		var value = 1
		for (i in 0 until FIELD_SIZE - 1) {
			exp[i] = value
			log[value] = i
			value = multiplyRaw(value, GENERATOR)
		}
		// The cycle has length 255; repeat it so exp[i + 255] == exp[i] for the doubled range.
		for (i in FIELD_SIZE - 1 until exp.size) {
			exp[i] = exp[i - (FIELD_SIZE - 1)]
		}
		// log[0] is undefined in the field; multiply/divide handle 0 before ever reading it.
	}

	// Only used to bootstrap the tables above.
	private fun multiplyRaw(a: Int, b: Int): Int {
		var left = a
		var right = b
		var result = 0
		while (right != 0) {
			if (right and 1 != 0) result = result xor left
			val carry = left and 0x80
			left = left shl 1
			if (carry != 0) left = left xor IRREDUCIBLE
			right = right shr 1
		}
		return result and 0xFF
	}

	fun add(a: Int, b: Int): Int = a xor b

	// Subtraction is addition: every element is its own additive inverse in characteristic 2.
	fun subtract(a: Int, b: Int): Int = a xor b

	fun multiply(a: Int, b: Int): Int {
		if (a == 0 || b == 0) return 0
		return exp[log[a] + log[b]]
	}

	fun divide(a: Int, b: Int): Int {
		require(b != 0) { "Division by zero is undefined in GF(256)." }
		if (a == 0) return 0
		// + 255 keeps the index non-negative without a branch on the sign of the difference.
		return exp[log[a] - log[b] + (FIELD_SIZE - 1)]
	}
}
