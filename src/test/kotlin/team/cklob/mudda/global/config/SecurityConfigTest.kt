package team.cklob.mudda.global.config

import io.swagger.v3.oas.annotations.Hidden
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import com.ninjasquad.springmockk.MockkBean
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(controllers = [SecurityTestController::class], properties = [
    "jwt.secret=local-test-secret-must-be-at-least-32-bytes",
])
@Import(SecurityConfig::class, JwtTokenProvider::class)
class SecurityConfigTest(@Autowired private val mockMvc: MockMvc, @Autowired private val jwtTokenProvider: JwtTokenProvider) {
    @MockkBean lateinit var jpaMappingContext: JpaMetamodelMappingContext
    @MockkBean lateinit var accessTokenBlacklist: AccessTokenBlacklist

    @Test fun `permits public map path and rejects protected path without authentication`() {
        every { accessTokenBlacklist.isBlacklisted(any()) } returns false
        every { accessTokenBlacklist.isRevoked(any(), any()) } returns false
        mockMvc.perform(get("/api/v1/maps/ping")).andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/private/ping")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/private/ping").header("Authorization", "Bearer ${jwtTokenProvider.createAccessToken(1)}")).andExpect(status().isOk)
    }
}

// @Hidden keeps this fixture out of the generated OpenAPI document. Component scanning pulls it into
// any full @SpringBootTest context, where it would otherwise show up as a real endpoint alongside the
// actual API (see OpenApiDocumentTest).
@Hidden
@RestController
class SecurityTestController {
    @GetMapping("/api/v1/maps/ping") fun public() = "ok"
    @GetMapping("/api/v1/private/ping") fun private() = "ok"
}
