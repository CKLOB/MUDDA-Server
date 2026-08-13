package team.cklob.mudda.global.crypto

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

// Holds the master key (KEK) used to wrap per-value data keys. The raw key never leaves this class as
// text -- toString is overridden so an accidental log of the bean cannot leak it, and only the derived
// SecretKey is exposed.
//
// Validation happens in the constructor on purpose: a wrong-length key must fail the application at
// startup, not on the first capsule someone tries to save.
@ConfigurationProperties("mudda.crypto")
class CryptoProperties(masterKey: String) {
    val kek: SecretKey = toSecretKey(masterKey)

    private fun toSecretKey(encoded: String): SecretKey {
        val bytes = try {
            Base64.getDecoder().decode(encoded)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("mudda.crypto.master-key must be Base64-encoded.", e)
        }
        check(bytes.size == KEY_LENGTH_BYTES) {
            "mudda.crypto.master-key must decode to $KEY_LENGTH_BYTES bytes but was ${bytes.size}."
        }
        return SecretKeySpec(bytes, "AES")
    }

    override fun toString(): String = "CryptoProperties(masterKey=***)"

    companion object {
        const val KEY_LENGTH_BYTES = 32
    }
}
