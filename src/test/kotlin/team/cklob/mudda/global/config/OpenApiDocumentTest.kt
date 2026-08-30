package team.cklob.mudda.global.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import team.cklob.mudda.support.PostgresIntegrationTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Swagger annotations are easy to add and just as easy to leave half-finished, so the generated document
// is asserted directly rather than trusting that the annotations are present in the source.
@AutoConfigureMockMvc
class OpenApiDocumentTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val objectMapper: ObjectMapper,
) : PostgresIntegrationTest() {
	private fun document(): JsonNode {
		val body = mockMvc.perform(get("/v3/api-docs")).andReturn().response.contentAsString
		return objectMapper.readTree(body)
	}

	// SecurityConfigTest declares a @RestController fixture that component scanning pulls into this
	// context, so it appears in the generated document during tests but never in production. It is not
	// part of the API under test.
	private fun JsonNode.productionOperations(): List<Pair<String, JsonNode>> =
		path("paths").fields().asSequence().flatMap { (path, methods) ->
			methods.fields().asSequence().map { (method, operation) -> "$method $path" to operation }
		}.filterNot { (_, operation) ->
			operation.path("tags").any { it.asText() in TEST_ONLY_TAGS }
		}.toList()

	private companion object {
		val TEST_ONLY_TAGS = setOf("security-test-controller")
	}

	@Test fun `every endpoint is documented with a summary`() {
		val undocumented = document().productionOperations()
			.filter { (_, operation) -> operation.path("summary").asText("").isBlank() }
			.map { (endpoint, _) -> endpoint }

		assertTrue(undocumented.isEmpty(), "endpoints missing an @Operation summary: $undocumented")
	}

	@Test fun `every endpoint is grouped under a tag`() {
		val untagged = document().productionOperations()
			.filterNot { (_, operation) -> operation.path("tags").elements().hasNext() }
			.map { (endpoint, _) -> endpoint }

		assertTrue(untagged.isEmpty(), "endpoints missing a @Tag: $untagged")
	}

	@Test fun `all nine domains are present as tags`() {
		val tags = document().productionOperations()
			.flatMap { (_, operation) -> operation.path("tags").map { it.asText() } }
			.toSet()

		assertEquals(
			setOf("Auth", "Member", "Media", "Capsule", "Friend", "Notification", "Feed", "Block", "Report"),
			tags,
		)
	}

	@Test fun `every documented endpoint carries a description as well as a summary`() {
		val missing = document().productionOperations()
			.filter { (_, operation) -> operation.path("description").asText("").isBlank() }
			.map { (endpoint, _) -> endpoint }

		assertTrue(missing.isEmpty(), "endpoints missing an @Operation description: $missing")
	}

	@Test fun `the bearer security scheme is registered so Authorize works in the UI`() {
		val scheme = document().path("components").path("securitySchemes").path("bearerAuth")

		assertEquals("http", scheme.path("type").asText())
		assertEquals("bearer", scheme.path("scheme").asText())
		assertEquals("JWT", scheme.path("bearerFormat").asText())
	}

	@Test fun `the api carries a title and version`() {
		val info = document().path("info")

		assertEquals("MUDDA API", info.path("title").asText())
		assertEquals("v1", info.path("version").asText())
	}
}
