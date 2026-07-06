package team.cklob.mudda

import org.junit.jupiter.api.Test
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
	properties = [
		"spring.cloud.aws.region.static=ap-northeast-2",
		"spring.cloud.aws.credentials.access-key=test",
		"spring.cloud.aws.credentials.secret-key=test",
	],
)
@Testcontainers
class MuddaApplicationTests {

	@Test
	fun contextLoads() {
	}

	companion object {
		@Container
		@ServiceConnection
		@JvmStatic
		val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
	}
}
