package team.cklob.mudda.global.crypto.shamir

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

// The field laws are what make Lagrange interpolation recover the right constant term; if any of them
// break, split/combine fails in ways that look like corrupted data rather than a math bug.
class GaloisField256Test {
	@Test fun `addition is xor and is its own inverse`() {
		for (a in 0..255) {
			for (b in 0..255) {
				assertEquals(a xor b, GaloisField256.add(a, b))
				assertEquals(a, GaloisField256.subtract(GaloisField256.add(a, b), b))
			}
		}
	}

	@Test fun `one is the multiplicative identity and zero annihilates`() {
		for (a in 0..255) {
			assertEquals(a, GaloisField256.multiply(a, 1))
			assertEquals(0, GaloisField256.multiply(a, 0))
			assertEquals(0, GaloisField256.multiply(0, a))
		}
	}

	@Test fun `multiplication is commutative and stays inside the field`() {
		for (a in 0..255) {
			for (b in 0..255) {
				val product = GaloisField256.multiply(a, b)
				assertEquals(GaloisField256.multiply(b, a), product)
				assertEquals(product, product and 0xFF, "a product escaped the byte range")
			}
		}
	}

	@Test fun `division undoes multiplication for every non-zero divisor`() {
		for (a in 0..255) {
			for (b in 1..255) {
				assertEquals(a, GaloisField256.divide(GaloisField256.multiply(a, b), b))
			}
		}
	}

	@Test fun `every non-zero element has a multiplicative inverse`() {
		for (a in 1..255) {
			assertEquals(1, GaloisField256.multiply(a, GaloisField256.divide(1, a)))
		}
	}

	@Test fun `multiplication is associative and distributes over addition`() {
		// A sampled sweep: the full 16.7M triple loop would dominate the suite's runtime.
		val values = (0..255 step 7).toList()
		for (a in values) {
			for (b in values) {
				for (c in values) {
					assertEquals(
						GaloisField256.multiply(GaloisField256.multiply(a, b), c),
						GaloisField256.multiply(a, GaloisField256.multiply(b, c)),
					)
					assertEquals(
						GaloisField256.multiply(a, GaloisField256.add(b, c)),
						GaloisField256.add(GaloisField256.multiply(a, b), GaloisField256.multiply(a, c)),
					)
				}
			}
		}
	}

	@Test fun `dividing by zero is rejected rather than returning a wrong element`() {
		assertThrows<IllegalArgumentException> { GaloisField256.divide(5, 0) }
	}
}
