package team.cklob.mudda.global.crypto

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Envelope encryption for values that must never reach the database in plaintext.
//
// Every value gets a freshly generated data key (DEK): the value is encrypted under that DEK, and the DEK
// is then wrapped with the server master key (KEK). Two reasons for the extra hop instead of encrypting
// straight under the KEK:
//   - rotating the KEK only means re-wrapping a ~60 byte DEK per row, not re-encrypting every body
//   - it keeps the volume encrypted under any single key small, so GCM nonce collisions stay a non-issue
//
// The output is a self-describing blob, so no extra columns are needed, and the leading version tag
// leaves room to introduce multi-key rotation later without another migration:
//
//   v1:<dekNonce>:<wrappedDek>:<contentNonce>:<ciphertext‖tag>     (every part Base64)
//
// ':' is not in the Base64 alphabet, so it is safe as a separator.
@Component
class ContentCipher(properties: CryptoProperties) {
    private val kek: SecretKey = properties.kek
    private val random = SecureRandom()

    fun encrypt(plaintext: String): String {
        val dek = randomBytes(KEY_LENGTH_BYTES)
        val dekNonce = randomBytes(NONCE_LENGTH_BYTES)
        val contentNonce = randomBytes(NONCE_LENGTH_BYTES)

        val wrappedDek = transform(Cipher.ENCRYPT_MODE, kek, dekNonce, dek)
        val ciphertext = transform(Cipher.ENCRYPT_MODE, aesKey(dek), contentNonce, plaintext.toByteArray(Charsets.UTF_8))

        return listOf(dekNonce, wrappedDek, contentNonce, ciphertext)
            .joinToString(SEPARATOR, prefix = VERSION + SEPARATOR) { encode(it) }
    }

    fun decrypt(blob: String): String {
        val parts = blob.split(SEPARATOR)
        // Never include the blob itself in the message -- it would put ciphertext into the logs.
        require(parts.size == PART_COUNT && parts[0] == VERSION) { "Unsupported encrypted value format." }

        val dek = transform(Cipher.DECRYPT_MODE, kek, decode(parts[1]), decode(parts[2]))
        val plaintext = transform(Cipher.DECRYPT_MODE, aesKey(dek), decode(parts[3]), decode(parts[4]))

        return String(plaintext, Charsets.UTF_8)
    }

    // GCM authentication failures propagate as-is (AEADBadTagException). Tampered ciphertext must fail
    // loudly rather than be swallowed into a null or an empty string.
    private fun transform(mode: Int, key: SecretKey, nonce: ByteArray, input: ByteArray): ByteArray {
        // A fresh Cipher per call: javax.crypto.Cipher is not thread-safe and this bean is a singleton.
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(mode, key, GCMParameterSpec(TAG_LENGTH_BITS, nonce))
        return cipher.doFinal(input)
    }

    private fun randomBytes(length: Int): ByteArray = ByteArray(length).also(random::nextBytes)

    private fun aesKey(bytes: ByteArray): SecretKey = SecretKeySpec(bytes, "AES")

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    companion object {
        private const val VERSION = "v1"
        private const val SEPARATOR = ":"
        private const val PART_COUNT = 5
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
        private const val NONCE_LENGTH_BYTES = 12
        private const val KEY_LENGTH_BYTES = 32
    }
}
