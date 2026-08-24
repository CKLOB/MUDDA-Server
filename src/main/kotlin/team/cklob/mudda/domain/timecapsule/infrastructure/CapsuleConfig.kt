package team.cklob.mudda.domain.timecapsule.infrastructure

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import team.cklob.mudda.domain.timecapsule.application.CapsuleProperties

@Configuration
@EnableConfigurationProperties(CapsuleProperties::class)
class CapsuleConfig {
	@Bean
	fun capsulePasswordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
