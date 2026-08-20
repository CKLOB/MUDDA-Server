package team.cklob.mudda.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgisContainer(imageName: DockerImageName) : PostgreSQLContainer<PostgisContainer>(imageName)

// Shared base for repository integration tests that need a real PostgreSQL/PostGIS instance (real Flyway
// migrations, JPQL, unique/CHECK constraints -- things a MockK-based unit test cannot verify). Subclasses
// share this single container and the same @SpringBootTest bootstrap configuration, so Spring's context
// cache and the Testcontainers container are both reused across every subclass instead of each test class
// paying for its own container + full migration run.
//
// The container is deliberately NOT annotated with @Container/@Testcontainers. That JUnit-managed
// lifecycle is scoped per test class -- when two unrelated top-level classes both inherit the same
// @JvmStatic container field from this base, the extension stops it after the first class's tests finish,
// leaving the second class unable to connect. Starting it once here (on first access to the companion
// object, which the JVM guarantees happens at most once) and never stopping it is the standard
// Testcontainers "singleton container" pattern for sharing a container across multiple test classes; the
// Ryuk resource reaper Testcontainers registers internally still guarantees cleanup on JVM exit.
@SpringBootTest(
	properties = [
		"spring.cloud.aws.region.static=ap-northeast-2",
		"spring.cloud.aws.credentials.access-key=test",
		"spring.cloud.aws.credentials.secret-key=test",
		"jwt.secret=local-test-secret-must-be-at-least-32-bytes",
		"mudda.crypto.master-key=bXVkZGEtdGVzdC1tYXN0ZXIta2V5LTMyLWJ5dGVzISE=",
	],
)
@Transactional
abstract class PostgresIntegrationTest {
	companion object {
		@ServiceConnection
		@JvmStatic
		val postgres: PostgisContainer = PostgisContainer(
			DockerImageName.parse("postgis/postgis:16-3.5-alpine").asCompatibleSubstituteFor("postgres"),
		)
			.withInitScript("db/init/001_enable_postgis.sql")
			.also { it.start() }
	}
}
