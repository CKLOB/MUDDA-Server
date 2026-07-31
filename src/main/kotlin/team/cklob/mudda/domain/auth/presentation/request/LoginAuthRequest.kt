package team.cklob.mudda.domain.auth.presentation.request

import jakarta.validation.constraints.NotBlank

data class LoginAuthRequest(
    @field:NotBlank
    val code: String,

    @field:NotBlank
    val redirectUri: String,
)
