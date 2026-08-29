package team.cklob.mudda.domain.feed.presentation.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import team.cklob.mudda.domain.feed.application.impl.GetFeedListService
import team.cklob.mudda.domain.feed.domain.type.FeedType
import team.cklob.mudda.domain.feed.infrastructure.FeedBroadcaster
import team.cklob.mudda.domain.feed.presentation.response.FeedListResponse
import team.cklob.mudda.domain.feed.presentation.response.FeedMemberResponse
import team.cklob.mudda.domain.feed.presentation.response.FeedResponse
import team.cklob.mudda.global.config.SecurityConfig
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.LocalDateTime

@WebMvcTest(controllers = [FeedController::class], properties = [
	"jwt.secret=local-test-secret-must-be-at-least-32-bytes",
])
@Import(SecurityConfig::class, JwtTokenProvider::class, FeedBroadcaster::class)
class FeedControllerTest(@Autowired private val mockMvc: MockMvc, @Autowired private val jwtTokenProvider: JwtTokenProvider) {
	@MockkBean lateinit var jpaMappingContext: JpaMetamodelMappingContext
	@MockkBean lateinit var accessTokenBlacklist: AccessTokenBlacklist
	@MockkBean lateinit var getFeedListService: GetFeedListService

	private fun accessTokenFor(memberId: Long): String {
		every { accessTokenBlacklist.isBlacklisted(any()) } returns false
		every { accessTokenBlacklist.isRevoked(any(), any()) } returns false
		return jwtTokenProvider.createAccessToken(memberId)
	}

	@Test fun `getFeeds requires authentication`() {
		mockMvc.perform(get("/api/v1/feed")).andExpect(status().isUnauthorized)
	}

	@Test fun `getFeeds returns the spec-shaped payload`() {
		val token = accessTokenFor(1L)
		every { getFeedListService.execute(any()) } returns FeedListResponse(
			listOf(
				FeedResponse(
					feedId = 3, type = FeedType.CAPSULE_OPENED, message = "nick님이 '첫 캡슐'을(를) 발견했어요.",
					member = FeedMemberResponse(2, "nick", null), capsuleId = 9, createdAt = LocalDateTime.now(),
				),
			),
		)

		mockMvc.perform(get("/api/v1/feed").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.feeds[0].feedId").value(3))
			.andExpect(jsonPath("$.data.feeds[0].type").value("CAPSULE_OPENED"))
			.andExpect(jsonPath("$.data.feeds[0].member.memberId").value(2))
			.andExpect(jsonPath("$.data.feeds[0].capsuleId").value(9))
	}

	@Test fun `stream requires authentication`() {
		mockMvc.perform(get("/api/v1/feed/stream")).andExpect(status().isUnauthorized)
	}

	@Test fun `stream opens an SSE connection`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(get("/api/v1/feed/stream").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)
			.andExpect(request().asyncStarted())
	}
}
