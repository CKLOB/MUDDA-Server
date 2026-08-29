package team.cklob.mudda.domain.notification.infrastructure

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import team.cklob.mudda.domain.notification.application.NotificationSender

@Configuration
@EnableConfigurationProperties(FcmProperties::class)
class FcmConfig {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Bean
	fun firebaseApp(properties: FcmProperties, resourceLoader: ResourceLoader): FirebaseApp? {
		if (!properties.enabled) return null
		require(properties.credentials.isNotBlank()) { "fcm.enabled=true requires fcm.credentials" }
		// FirebaseApp.initializeApp throws if the default app already exists, which happens when the
		// context is rebuilt inside a single JVM (a @DirtiesContext test run, for instance).
		FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }?.let { return it }
		val credentials = resourceLoader.getResource(properties.credentials).inputStream.use(GoogleCredentials::fromStream)
		return FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).build())
	}

	@Bean
	fun notificationSender(firebaseApp: FirebaseApp?): NotificationSender {
		if (firebaseApp != null) return FcmNotificationSender(firebaseApp)
		logger.info("FCM is disabled; push notifications will be recorded in the database only")
		// ponytail: a no-op keeps every environment without Firebase credentials -- tests, CI, local dev --
		// on the exact same code path as production instead of scattering `if (fcmEnabled)` through the
		// domain.
		return object : NotificationSender {
			override fun send(tokens: List<String>, title: String, body: String) = emptyList<String>()
		}
	}
}
