package team.cklob.mudda.domain.member.presentation.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.application.impl.GetMemberProfileService
import team.cklob.mudda.domain.member.application.impl.GetMyMemberService
import team.cklob.mudda.domain.member.application.impl.UpdateMyMemberService
import team.cklob.mudda.domain.member.domain.type.Gender
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.member.presentation.request.UpdateMyMemberRequest
import team.cklob.mudda.domain.member.presentation.response.MemberProfileResponse
import team.cklob.mudda.domain.member.presentation.response.MyMemberResponse
import team.cklob.mudda.global.config.SecurityConfig
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.LocalDateTime

@WebMvcTest(controllers = [MemberController::class], properties = [
	"jwt.secret=local-test-secret-must-be-at-least-32-bytes",
])
@Import(SecurityConfig::class, JwtTokenProvider::class)
class MemberControllerTest(@Autowired private val mockMvc: MockMvc, @Autowired private val jwtTokenProvider: JwtTokenProvider) {
	@MockkBean lateinit var jpaMappingContext: JpaMetamodelMappingContext
	@MockkBean lateinit var accessTokenBlacklist: AccessTokenBlacklist
	@MockkBean lateinit var getMyMemberService: GetMyMemberService
	@MockkBean lateinit var updateMyMemberService: UpdateMyMemberService
	@MockkBean lateinit var getMemberProfileService: GetMemberProfileService

	private val now: LocalDateTime = LocalDateTime.now()

	private fun accessTokenFor(memberId: Long): String {
		every { accessTokenBlacklist.isBlacklisted(any()) } returns false
		every { accessTokenBlacklist.isRevoked(any(), any()) } returns false
		return jwtTokenProvider.createAccessToken(memberId)
	}

	private fun myMemberResponse() = MyMemberResponse(
		memberId = 1L, name = "name", nickname = "nickname", gender = Gender.MALE, birthYear = 2000,
		profileImageUrl = null, bio = null, profileVisibility = ProfileVisibility.PUBLIC, createdAt = now, updatedAt = now,
	)

	@Test fun `getMe requires authentication`() {
		mockMvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized)
	}

	@Test fun `getMe returns the authenticated member's data wrapped in the common envelope`() {
		val token = accessTokenFor(1L)
		every { getMyMemberService.execute(1L) } returns myMemberResponse()

		mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.memberId").value(1))
			.andExpect(jsonPath("$.data.nickname").value("nickname"))
			.andExpect(jsonPath("$.data.email").doesNotExist())
			.andExpect(jsonPath("$.data.oauthProvider").doesNotExist())
			.andExpect(jsonPath("$.data.providerId").doesNotExist())
	}

	@Test fun `updateMe requires authentication`() {
		mockMvc.perform(
			patch("/api/v1/members/me").contentType(MediaType.APPLICATION_JSON).content("""{"bio":"new bio"}"""),
		).andExpect(status().isUnauthorized)
	}

	@Test fun `updateMe rejects an out-of-range birthYear before reaching the service`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(
			patch("/api/v1/members/me")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"birthYear":1800}"""),
		).andExpect(status().isBadRequest)
	}

	@Test fun `updateMe returns the saved member's data`() {
		val token = accessTokenFor(1L)
		every { updateMyMemberService.execute(1L, UpdateMyMemberRequest(bio = "new bio")) } returns myMemberResponse().copy(bio = "new bio")

		mockMvc.perform(
			patch("/api/v1/members/me")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"bio":"new bio"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.bio").value("new bio"))
	}

	@Test fun `updateMe rejects a non-http(s) profileImageUrl before reaching the service`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(
			patch("/api/v1/members/me")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"profileImageUrl":"javascript:alert(1)"}"""),
		).andExpect(status().isBadRequest)
	}

	@Test fun `updateMe accepts an http(s) profileImageUrl and an empty string to clear it`() {
		val token = accessTokenFor(1L)
		every { updateMyMemberService.execute(1L, UpdateMyMemberRequest(profileImageUrl = "http://cdn.local/img.png")) } returns
			myMemberResponse().copy(profileImageUrl = "http://cdn.local/img.png")
		every { updateMyMemberService.execute(1L, UpdateMyMemberRequest(profileImageUrl = "")) } returns myMemberResponse().copy(profileImageUrl = null)

		mockMvc.perform(
			patch("/api/v1/members/me")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"profileImageUrl":"http://cdn.local/img.png"}"""),
		).andExpect(status().isOk)

		mockMvc.perform(
			patch("/api/v1/members/me")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"profileImageUrl":""}"""),
		).andExpect(status().isOk)
	}

	@Test fun `updateMe returns 409 when the nickname is already taken`() {
		val token = accessTokenFor(1L)
		every { updateMyMemberService.execute(1L, UpdateMyMemberRequest(nickname = "taken")) } throws BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS)

		mockMvc.perform(
			patch("/api/v1/members/me")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"nickname":"taken"}"""),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.error.code").value("M001"))
	}

	@Test fun `getProfile requires authentication`() {
		mockMvc.perform(get("/api/v1/members/2")).andExpect(status().isUnauthorized)
	}

	@Test fun `getProfile passes the authenticated viewer id and the path member id to the service`() {
		val token = accessTokenFor(1L)
		every { getMemberProfileService.execute(1L, 2L) } returns MemberProfileResponse(
			memberId = 2L, nickname = "other-nick", gender = Gender.FEMALE, birthYear = 1999,
			profileImageUrl = null, bio = null, friendStatus = FriendStatus.NONE, createdAt = now,
		)

		mockMvc.perform(get("/api/v1/members/2").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.memberId").value(2))
			.andExpect(jsonPath("$.data.friendStatus").value("NONE"))
			.andExpect(jsonPath("$.data.name").doesNotExist())
			.andExpect(jsonPath("$.data.email").doesNotExist())
			.andExpect(jsonPath("$.data.oauthProvider").doesNotExist())
			.andExpect(jsonPath("$.data.providerId").doesNotExist())
	}

	@Test fun `getProfile returns 404 when the member does not exist`() {
		val token = accessTokenFor(1L)
		every { getMemberProfileService.execute(1L, 99L) } throws BusinessException(ErrorCode.MEMBER_NOT_FOUND)

		mockMvc.perform(get("/api/v1/members/99").header("Authorization", "Bearer $token"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("M002"))
	}

	@Test fun `getProfile returns 403 when the profile is not accessible`() {
		val token = accessTokenFor(1L)
		every { getMemberProfileService.execute(1L, 2L) } throws BusinessException(ErrorCode.PROFILE_ACCESS_DENIED)

		mockMvc.perform(get("/api/v1/members/2").header("Authorization", "Bearer $token"))
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.error.code").value("M003"))
	}

	@Test fun `getProfile returns 400 for a non-numeric memberId instead of a 500`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(get("/api/v1/members/abc").header("Authorization", "Bearer $token"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.error.code").value("C001"))
	}
}
