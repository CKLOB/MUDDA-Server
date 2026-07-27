package team.cklob.mudda.domain.auth.presentation.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import team.cklob.mudda.domain.auth.application.impl.OAuthLoginService
import team.cklob.mudda.domain.auth.application.impl.ReissueService
import team.cklob.mudda.domain.auth.application.impl.SignoutService
import team.cklob.mudda.domain.auth.application.impl.SignupService
import team.cklob.mudda.domain.auth.application.impl.WithdrawService
import team.cklob.mudda.domain.auth.presentation.request.OAuthLoginRequest
import team.cklob.mudda.domain.auth.presentation.response.OAuthLoginResponse
import team.cklob.mudda.domain.auth.presentation.response.TokenResponse
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.config.SecurityConfig
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider

@WebMvcTest(controllers = [AuthController::class], properties = [
    "jwt.secret=local-test-secret-must-be-at-least-32-bytes",
])
@Import(SecurityConfig::class, JwtTokenProvider::class)
class AuthControllerTest(@Autowired private val mockMvc: MockMvc, @Autowired private val jwtTokenProvider: JwtTokenProvider) {
    @MockkBean lateinit var jpaMappingContext: JpaMetamodelMappingContext
    @MockkBean lateinit var accessTokenBlacklist: AccessTokenBlacklist
    @MockkBean lateinit var oauthLoginService: OAuthLoginService
    @MockkBean lateinit var signupService: SignupService
    @MockkBean lateinit var reissueService: ReissueService
    @MockkBean lateinit var signoutService: SignoutService
    @MockkBean lateinit var withdrawService: WithdrawService

    @Test fun `oauth login endpoint is public and returns the service result`() {
        every { accessTokenBlacklist.isBlacklisted(any()) } returns false
        every { oauthLoginService.execute(OAuthProvider.GOOGLE, OAuthLoginRequest("auth-code", "https://app.mudda.com/oauth/callback")) } returns
            OAuthLoginResponse(accessToken = "access-token", refreshToken = "refresh-token", isNewMember = true)

        mockMvc.perform(
            post("/api/v1/auth/oauth/GOOGLE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"auth-code","providerUri":"https://app.mudda.com/oauth/callback"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.isNewMember").value(true))
    }

    @Test fun `signup endpoint requires authentication`() {
        mockMvc.perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"name","nickname":"nickname","gender":"MALE","age":20}"""),
        ).andExpect(status().isUnauthorized)
    }

    @Test fun `signup endpoint returns 201 with no body when authenticated`() {
        every { accessTokenBlacklist.isBlacklisted(any()) } returns false
        every { signupService.execute(1L, any()) } just runs
        val accessToken = jwtTokenProvider.createAccessToken(1L)

        mockMvc.perform(
            post("/api/v1/auth/signup")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"name","nickname":"nickname","gender":"MALE","age":20}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$").doesNotExist())
    }

    @Test fun `reissue endpoint is public and reads the custom refreshToken header`() {
        every { reissueService.execute("raw-refresh-token") } returns TokenResponse("new-access-token", "new-refresh-token")

        mockMvc.perform(patch("/api/v1/auth/reissue").header("refreshToken", "Bearer raw-refresh-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
    }

    @Test fun `signout endpoint requires authentication and returns 204`() {
        every { accessTokenBlacklist.isBlacklisted(any()) } returns false
        every { signoutService.execute(1L, any()) } just runs
        val accessToken = jwtTokenProvider.createAccessToken(1L)

        mockMvc.perform(delete("/api/v1/auth/signout").header("Authorization", "Bearer $accessToken"))
            .andExpect(status().isNoContent)
    }

    @Test fun `withdraw endpoint requires authentication and returns 204`() {
        every { accessTokenBlacklist.isBlacklisted(any()) } returns false
        every { withdrawService.execute(1L, any()) } just runs
        val accessToken = jwtTokenProvider.createAccessToken(1L)

        mockMvc.perform(delete("/api/v1/auth/withdraw").header("Authorization", "Bearer $accessToken"))
            .andExpect(status().isNoContent)
    }
}
