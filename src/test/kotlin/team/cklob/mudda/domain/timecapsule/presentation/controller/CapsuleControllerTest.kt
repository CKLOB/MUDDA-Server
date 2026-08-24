package team.cklob.mudda.domain.timecapsule.presentation.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import team.cklob.mudda.domain.timecapsule.application.impl.CreateCapsuleService
import team.cklob.mudda.domain.timecapsule.application.impl.CreateGuestbookService
import team.cklob.mudda.domain.timecapsule.application.impl.DeleteCapsuleService
import team.cklob.mudda.domain.timecapsule.application.impl.DeleteGuestbookService
import team.cklob.mudda.domain.timecapsule.application.impl.GetCapsuleDetailService
import team.cklob.mudda.domain.timecapsule.application.impl.GetCapsuleListService
import team.cklob.mudda.domain.timecapsule.application.impl.GetGuestbookListService
import team.cklob.mudda.domain.timecapsule.application.impl.GetMyCapsuleListService
import team.cklob.mudda.domain.timecapsule.application.impl.GetNearbyCapsuleService
import team.cklob.mudda.domain.timecapsule.application.impl.GetReceivedCapsuleListService
import team.cklob.mudda.domain.timecapsule.application.impl.OpenCapsuleService
import team.cklob.mudda.domain.timecapsule.application.impl.UpdateGuestbookService
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.domain.timecapsule.presentation.request.CreateCapsuleRequest
import team.cklob.mudda.domain.timecapsule.presentation.response.CreateCapsuleResponse
import team.cklob.mudda.global.config.SecurityConfig
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.LocalDateTime

@WebMvcTest(controllers = [CapsuleController::class], properties = ["jwt.secret=local-test-secret-must-be-at-least-32-bytes"])
@Import(SecurityConfig::class, JwtTokenProvider::class)
class CapsuleControllerTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val jwtTokenProvider: JwtTokenProvider,
) {
	@MockkBean lateinit var jpaMappingContext: JpaMetamodelMappingContext
	@MockkBean lateinit var accessTokenBlacklist: AccessTokenBlacklist
	@MockkBean lateinit var createCapsuleService: CreateCapsuleService
	@MockkBean lateinit var getCapsuleListService: GetCapsuleListService
	@MockkBean lateinit var getNearbyCapsuleService: GetNearbyCapsuleService
	@MockkBean lateinit var getCapsuleDetailService: GetCapsuleDetailService
	@MockkBean lateinit var getMyCapsuleListService: GetMyCapsuleListService
	@MockkBean lateinit var getReceivedCapsuleListService: GetReceivedCapsuleListService
	@MockkBean lateinit var openCapsuleService: OpenCapsuleService
	@MockkBean lateinit var deleteCapsuleService: DeleteCapsuleService
	@MockkBean lateinit var createGuestbookService: CreateGuestbookService
	@MockkBean lateinit var getGuestbookListService: GetGuestbookListService
	@MockkBean lateinit var updateGuestbookService: UpdateGuestbookService
	@MockkBean lateinit var deleteGuestbookService: DeleteGuestbookService

	private fun token(memberId: Long): String {
		every { accessTokenBlacklist.isBlacklisted(any()) } returns false
		every { accessTokenBlacklist.isRevoked(any(), any()) } returns false
		return jwtTokenProvider.createAccessToken(memberId)
	}

	@Test
	fun `capsule endpoints require authentication`() {
		mockMvc.perform(post("/api/v1/capsule").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `creates capsule with created status`() {
		val openAt = LocalDateTime.of(2030, 1, 1, 12, 0)
		val request = CreateCapsuleRequest(
			name = "future", content = "secret", latitude = 37.5, longitude = 127.0,
			openAt = openAt, visibility = CapsuleVisibility.PRIVATE, lockType = CapsuleLockType.NONE,
		)
		every { createCapsuleService.execute(7, request) } returns
			CreateCapsuleResponse(1, "future", 37.5, 127.0, openAt, null, openAt.minusDays(1))

		mockMvc.perform(
			post("/api/v1/capsule")
				.header("Authorization", "Bearer ${token(7)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""{"name":"future","content":"secret","latitude":37.5,"longitude":127.0,"openAt":"2030-01-01T12:00:00","visibility":"PRIVATE","lockType":"NONE"}""",
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.capsuleId").value(1))
			.andExpect(jsonPath("$.data.title").value("future"))
	}
}
