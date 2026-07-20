package team.cklob.mudda.global.config

import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import com.ninjasquad.springmockk.MockkBean
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

    @Test fun `permits public map path and rejects protected path without authentication`() {
        mockMvc.perform(get("/api/v1/maps/ping")).andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/private/ping")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/private/ping").header("Authorization", "Bearer ${jwtTokenProvider.createAccessToken(1)}")).andExpect(status().isOk)
    }
}

@RestController
class SecurityTestController {
    @GetMapping("/api/v1/maps/ping") fun public() = "ok"
    @GetMapping("/api/v1/private/ping") fun private() = "ok"
}
