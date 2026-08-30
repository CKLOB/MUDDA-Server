package team.cklob.mudda.domain.notification.infrastructure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fcm")
data class FcmProperties(
	// Off by default so the app boots without Firebase credentials (tests, local dev, CI). Turning it on
	// without a usable credentials source fails fast at startup rather than silently dropping pushes.
	val enabled: Boolean = false,
	// Spring resource location of the service-account JSON, e.g. file:/etc/mudda/firebase.json or
	// classpath:firebase.json.
	val credentials: String = "",
)
