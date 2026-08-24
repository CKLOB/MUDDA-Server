package team.cklob.mudda.domain.timecapsule.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("capsule")
data class CapsuleProperties(
	val openRadiusMeter: Int = 100,
	val maxActivePerMember: Long = 100,
	val maxExpirationYears: Long = 10,
	val maxNearbyRadiusMeter: Double = 5_000.0,
)
