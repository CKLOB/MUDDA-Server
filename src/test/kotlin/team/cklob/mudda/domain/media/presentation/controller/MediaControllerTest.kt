package team.cklob.mudda.domain.media.presentation.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import team.cklob.mudda.domain.media.application.impl.CompleteMediaUploadService
import team.cklob.mudda.domain.media.application.impl.CreateMediaUploadUrlService
import team.cklob.mudda.domain.media.application.impl.DeleteMediaService
import team.cklob.mudda.domain.media.presentation.request.CompleteMediaUploadRequest
import team.cklob.mudda.domain.media.presentation.request.CreateMediaUploadUrlRequest
import team.cklob.mudda.domain.media.presentation.response.CompleteMediaUploadResponse
import team.cklob.mudda.domain.media.presentation.response.CreateMediaUploadUrlResponse
import team.cklob.mudda.global.config.SecurityConfig
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.LocalDateTime

@WebMvcTest(controllers = [MediaController::class], properties = [
	"jwt.secret=local-test-secret-must-be-at-least-32-bytes",
])
@Import(SecurityConfig::class, JwtTokenProvider::class)
class MediaControllerTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val jwtTokenProvider: JwtTokenProvider,
) {
	@MockkBean lateinit var jpaMappingContext: JpaMetamodelMappingContext
	@MockkBean lateinit var accessTokenBlacklist: AccessTokenBlacklist
	@MockkBean lateinit var createMediaUploadUrlService: CreateMediaUploadUrlService
	@MockkBean lateinit var completeMediaUploadService: CompleteMediaUploadService
	@MockkBean lateinit var deleteMediaService: DeleteMediaService

	private fun accessTokenFor(memberId: Long): String {
		every { accessTokenBlacklist.isBlacklisted(any()) } returns false
		every { accessTokenBlacklist.isRevoked(any(), any()) } returns false
		return jwtTokenProvider.createAccessToken(memberId)
	}

	@Test
	fun `media endpoints require authentication`() {
		mockMvc.perform(
			post("/api/v1/media/upload-urls")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"mediaType":"IMAGE","contentType":"image/jpeg","fileSize":100}"""),
		).andExpect(status().isUnauthorized)

		mockMvc.perform(delete("/api/v1/media/1")).andExpect(status().isUnauthorized)
	}

	@Test
	fun `creates an upload URL`() {
		val token = accessTokenFor(7)
		val request = CreateMediaUploadUrlRequest(team.cklob.mudda.domain.media.domain.type.MediaType.IMAGE, "image/jpeg", 100)
		every { createMediaUploadUrlService.execute(7, request) } returns
			CreateMediaUploadUrlResponse("pending/7/image/id", "https://upload", LocalDateTime.now().plusMinutes(10))

		mockMvc.perform(
			post("/api/v1/media/upload-urls")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"mediaType":"IMAGE","contentType":"image/jpeg","fileSize":100}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.uploadKey").value("pending/7/image/id"))
			.andExpect(jsonPath("$.data.uploadUrl").value("https://upload"))
	}

	@Test
	fun `rejects an invalid upload URL request before the service`() {
		val token = accessTokenFor(7)

		mockMvc.perform(
			post("/api/v1/media/upload-urls")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"mediaType":"IMAGE","contentType":"","fileSize":0}"""),
		).andExpect(status().isBadRequest)
	}

	@Test
	fun `completes an upload with created status`() {
		val token = accessTokenFor(7)
		val request = CompleteMediaUploadRequest("pending/7/image/id")
		val now = LocalDateTime.now()
		every { completeMediaUploadService.execute(7, request) } returns CompleteMediaUploadResponse(
			mediaId = 1,
			accessUrl = "https://access",
			accessUrlExpiresAt = now.plusMinutes(5),
			mediaType = team.cklob.mudda.domain.media.domain.type.MediaType.IMAGE,
			createdAt = now,
		)

		mockMvc.perform(
			post("/api/v1/media")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"uploadKey":"pending/7/image/id"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.mediaId").value(1))
			.andExpect(jsonPath("$.data.accessUrl").value("https://access"))
	}

	@Test
	fun `deletes media with no content`() {
		val token = accessTokenFor(7)
		every { deleteMediaService.execute(7, 1) } returns Unit

		mockMvc.perform(delete("/api/v1/media/1").header("Authorization", "Bearer $token"))
			.andExpect(status().isNoContent)
	}
}
