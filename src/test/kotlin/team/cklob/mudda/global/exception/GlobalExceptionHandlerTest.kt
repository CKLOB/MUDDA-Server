package team.cklob.mudda.global.exception

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class GlobalExceptionHandlerTest {
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(TestController()).setControllerAdvice(GlobalExceptionHandler()).build()

    @Test fun `maps business validation and unexpected exceptions safely`() {
        mockMvc.perform(get("/business")).andExpect(status().isNotFound).andExpect(jsonPath("$.error.code").value("T001"))
        mockMvc.perform(post("/valid").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.error.code").value("C001"))
        mockMvc.perform(get("/unexpected")).andExpect(status().isInternalServerError).andExpect(jsonPath("$.error.message").value("Internal server error."))
    }

    @RestController
    private class TestController {
        @GetMapping("/business") fun business(): Nothing = throw CapsuleException()
        @PostMapping("/valid") fun valid(@Valid @RequestBody body: Body) = body
        @GetMapping("/unexpected") fun unexpected(): Nothing = error("boom")
    }
    private data class Body(@field:NotBlank val value: String?)
}
