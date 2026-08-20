package team.cklob.mudda.global.crypto

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.stereotype.Component

// Attach with @Convert to any entity field that must never reach the database in plaintext.
//
// The boundary lives here rather than in the service layer on purpose: encryption a service has to
// remember to call is encryption a service will eventually forget to call. Hibernate resolves this
// converter through Spring's BeanContainer (auto-configured by Spring Boot), which is what lets it take
// ContentCipher as a constructor dependency -- EncryptedContentIntegrationTest guards that wiring.
//
// Note for callers: a converted column cannot be filtered or sorted on in SQL, and every entity load
// decrypts. List queries should project away the encrypted field rather than hydrate whole entities.
@Component
@Converter
class EncryptedStringConverter(private val contentCipher: ContentCipher) : AttributeConverter<String?, String?> {
    override fun convertToDatabaseColumn(attribute: String?): String? = attribute?.let(contentCipher::encrypt)

    override fun convertToEntityAttribute(dbData: String?): String? = dbData?.let(contentCipher::decrypt)
}
