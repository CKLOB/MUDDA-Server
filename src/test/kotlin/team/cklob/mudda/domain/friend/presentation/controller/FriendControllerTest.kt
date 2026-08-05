package team.cklob.mudda.domain.friend.presentation.controller

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import team.cklob.mudda.domain.friend.application.impl.DeleteFriendService
import team.cklob.mudda.domain.friend.application.impl.GetFriendListService
import team.cklob.mudda.domain.friend.application.impl.GetFriendRequestListService
import team.cklob.mudda.domain.friend.application.impl.RespondFriendRequestService
import team.cklob.mudda.domain.friend.application.impl.SearchFriendService
import team.cklob.mudda.domain.friend.application.impl.SendFriendRequestService
import team.cklob.mudda.domain.friend.domain.type.FriendRequestAction
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.domain.type.FriendRequestType
import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.friend.presentation.request.RespondFriendRequestRequest
import team.cklob.mudda.domain.friend.presentation.request.SendFriendRequestRequest
import team.cklob.mudda.domain.friend.presentation.response.FriendPageResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendRequestResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendSearchResponse
import team.cklob.mudda.domain.friend.presentation.response.SendFriendRequestResponse
import team.cklob.mudda.global.config.SecurityConfig
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.LocalDateTime

@WebMvcTest(controllers = [FriendController::class], properties = [
	"jwt.secret=local-test-secret-must-be-at-least-32-bytes",
])
@Import(SecurityConfig::class, JwtTokenProvider::class)
class FriendControllerTest(@Autowired private val mockMvc: MockMvc, @Autowired private val jwtTokenProvider: JwtTokenProvider) {
	@MockkBean lateinit var jpaMappingContext: JpaMetamodelMappingContext
	@MockkBean lateinit var accessTokenBlacklist: AccessTokenBlacklist
	@MockkBean lateinit var getFriendListService: GetFriendListService
	@MockkBean lateinit var searchFriendService: SearchFriendService
	@MockkBean lateinit var sendFriendRequestService: SendFriendRequestService
	@MockkBean lateinit var getFriendRequestListService: GetFriendRequestListService
	@MockkBean lateinit var respondFriendRequestService: RespondFriendRequestService
	@MockkBean lateinit var deleteFriendService: DeleteFriendService

	private val now: LocalDateTime = LocalDateTime.now()

	private fun accessTokenFor(memberId: Long): String {
		every { accessTokenBlacklist.isBlacklisted(any()) } returns false
		every { accessTokenBlacklist.isRevoked(any(), any()) } returns false
		return jwtTokenProvider.createAccessToken(memberId)
	}

	// -------- GET /api/v1/friends --------

	@Test fun `getFriends requires authentication`() {
		mockMvc.perform(get("/api/v1/friends")).andExpect(status().isUnauthorized)
	}

	@Test fun `getFriends returns the authenticated member's friend page`() {
		val token = accessTokenFor(1L)
		val page = FriendPageResponse(
			content = listOf(FriendResponse(memberId = 2L, nickname = "nick", profileImageUrl = null, acceptedAt = now)),
			page = 0, size = 20, totalElements = 1, totalPages = 1, hasNext = false,
		)
		every { getFriendListService.execute(1L, any()) } returns page

		mockMvc.perform(get("/api/v1/friends").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content[0].memberId").value(2))
			.andExpect(jsonPath("$.data.content[0].nickname").value("nick"))
	}

	// -------- GET /api/v1/friends/search --------

	@Test fun `search requires authentication`() {
		mockMvc.perform(get("/api/v1/friends/search").param("keyword", "nick")).andExpect(status().isUnauthorized)
	}

	@Test fun `search returns 400 when the keyword parameter is missing`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(get("/api/v1/friends/search").header("Authorization", "Bearer $token"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("C001"))
	}

	@Test fun `search returns candidates with relation status`() {
		val token = accessTokenFor(1L)
		val page = FriendPageResponse(
			content = listOf(FriendSearchResponse(memberId = 2L, nickname = "nick", profileImageUrl = null, relationStatus = FriendStatus.NONE, requestId = null, requestDirection = null)),
			page = 0, size = 20, totalElements = 1, totalPages = 1, hasNext = false,
		)
		every { searchFriendService.execute(1L, "nick", any()) } returns page

		mockMvc.perform(get("/api/v1/friends/search").param("keyword", "nick").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.content[0].relationStatus").value("NONE"))
	}

	@Test fun `search returns 400 when the service rejects a blank keyword`() {
		val token = accessTokenFor(1L)
		every { searchFriendService.execute(1L, " ", any()) } throws BusinessException(ErrorCode.INVALID_SEARCH_KEYWORD)

		mockMvc.perform(get("/api/v1/friends/search").param("keyword", " ").header("Authorization", "Bearer $token"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("F010"))
	}

	// -------- POST /api/v1/friends/requests --------

	@Test fun `sendRequest requires authentication`() {
		mockMvc.perform(
			post("/api/v1/friends/requests").contentType(MediaType.APPLICATION_JSON).content("""{"receiverId":2}"""),
		).andExpect(status().isUnauthorized)
	}

	@Test fun `sendRequest creates a request and returns 201`() {
		val token = accessTokenFor(1L)
		every { sendFriendRequestService.execute(1L, SendFriendRequestRequest(receiverId = 2L)) } returns SendFriendRequestResponse(requestId = 10L, status = FriendRequestStatus.PENDING)

		mockMvc.perform(
			post("/api/v1/friends/requests").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"receiverId":2}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.data.requestId").value(10))
			.andExpect(jsonPath("$.data.status").value("PENDING"))
	}

	@Test fun `sendRequest returns 400 for a malformed body`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(
			post("/api/v1/friends/requests").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{}"""),
		).andExpect(status().isBadRequest)
	}

	@Test fun `sendRequest returns 409 when a business rule rejects the request`() {
		val token = accessTokenFor(1L)
		every { sendFriendRequestService.execute(1L, SendFriendRequestRequest(receiverId = 2L)) } throws BusinessException(ErrorCode.ALREADY_FRIENDS)

		mockMvc.perform(
			post("/api/v1/friends/requests").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"receiverId":2}"""),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.error.code").value("F003"))
	}

	// -------- GET /api/v1/friends/requests --------

	@Test fun `getRequests requires authentication`() {
		mockMvc.perform(get("/api/v1/friends/requests").param("type", "RECEIVED")).andExpect(status().isUnauthorized)
	}

	@Test fun `getRequests returns 400 when type is missing`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(get("/api/v1/friends/requests").header("Authorization", "Bearer $token"))
			.andExpect(status().isBadRequest)
	}

	@Test fun `getRequests returns 400 for an invalid type value`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(get("/api/v1/friends/requests").param("type", "NOT_A_TYPE").header("Authorization", "Bearer $token"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("C001"))
	}

	@Test fun `getRequests defaults status to PENDING and returns the requested type`() {
		val token = accessTokenFor(1L)
		val page = FriendPageResponse(
			content = listOf(FriendRequestResponse(requestId = 10L, direction = FriendRequestType.RECEIVED, memberId = 2L, nickname = "nick", profileImageUrl = null, createdAt = now, status = FriendRequestStatus.PENDING)),
			page = 0, size = 20, totalElements = 1, totalPages = 1, hasNext = false,
		)
		every { getFriendRequestListService.execute(1L, FriendRequestType.RECEIVED, FriendRequestStatus.PENDING, any()) } returns page

		mockMvc.perform(get("/api/v1/friends/requests").param("type", "RECEIVED").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.content[0].direction").value("RECEIVED"))
			.andExpect(jsonPath("$.data.content[0].requestId").value(10))
	}

	// -------- PATCH /api/v1/friends/requests/{requestId} --------

	@Test fun `respondToRequest requires authentication`() {
		mockMvc.perform(
			patch("/api/v1/friends/requests/10").contentType(MediaType.APPLICATION_JSON).content("""{"action":"ACCEPT"}"""),
		).andExpect(status().isUnauthorized)
	}

	@Test fun `respondToRequest accepts and returns 204`() {
		val token = accessTokenFor(2L)
		every { respondFriendRequestService.execute(2L, 10L, RespondFriendRequestRequest(FriendRequestAction.ACCEPT)) } returns Unit

		mockMvc.perform(
			patch("/api/v1/friends/requests/10").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"action":"ACCEPT"}"""),
		).andExpect(status().isNoContent)
	}

	@Test fun `respondToRequest returns 400 for a missing action`() {
		val token = accessTokenFor(2L)

		mockMvc.perform(
			patch("/api/v1/friends/requests/10").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{}"""),
		).andExpect(status().isBadRequest)
	}

	@Test fun `respondToRequest returns 400 for an unsupported action value`() {
		val token = accessTokenFor(2L)

		mockMvc.perform(
			patch("/api/v1/friends/requests/10").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"action":"MAYBE"}"""),
		).andExpect(status().isBadRequest)
	}

	@Test fun `respondToRequest returns 403 when the caller is not the receiver`() {
		val token = accessTokenFor(1L)
		every { respondFriendRequestService.execute(1L, 10L, RespondFriendRequestRequest(FriendRequestAction.ACCEPT)) } throws
			BusinessException(ErrorCode.FRIEND_REQUEST_NOT_RECEIVER)

		mockMvc.perform(
			patch("/api/v1/friends/requests/10").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"action":"ACCEPT"}"""),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.error.code").value("F006"))
	}

	@Test fun `respondToRequest returns 404 when the request does not exist`() {
		val token = accessTokenFor(2L)
		every { respondFriendRequestService.execute(2L, 99L, RespondFriendRequestRequest(FriendRequestAction.ACCEPT)) } throws
			BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND)

		mockMvc.perform(
			patch("/api/v1/friends/requests/99").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"action":"ACCEPT"}"""),
		).andExpect(status().isNotFound)
	}

	// -------- DELETE /api/v1/friends/{memberId} --------

	@Test fun `deleteFriend requires authentication`() {
		mockMvc.perform(delete("/api/v1/friends/2")).andExpect(status().isUnauthorized)
	}

	@Test fun `deleteFriend passes the authenticated caller and target id and returns 204`() {
		val token = accessTokenFor(1L)
		every { deleteFriendService.execute(1L, 2L) } returns Unit

		mockMvc.perform(delete("/api/v1/friends/2").header("Authorization", "Bearer $token"))
			.andExpect(status().isNoContent)
	}

	@Test fun `deleteFriend returns 404 when there is no accepted friendship`() {
		val token = accessTokenFor(1L)
		every { deleteFriendService.execute(1L, 2L) } throws BusinessException(ErrorCode.FRIEND_NOT_FOUND)

		mockMvc.perform(delete("/api/v1/friends/2").header("Authorization", "Bearer $token"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.error.code").value("F008"))
	}

	@Test fun `deleteFriend returns 400 for a non-numeric memberId instead of a 500`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(delete("/api/v1/friends/abc").header("Authorization", "Bearer $token"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("C001"))
	}
}
