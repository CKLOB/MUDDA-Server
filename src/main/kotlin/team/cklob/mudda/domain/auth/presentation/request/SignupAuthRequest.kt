package team.cklob.mudda.domain.auth.presentation.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import team.cklob.mudda.domain.member.domain.type.Gender

data class SignupAuthRequest(
    @field:NotBlank
    @field:Size(max = 30)
    val name: String,

    @field:NotBlank
    @field:Size(max = 30)
    val nickname: String,

    val gender: Gender,

    val age: Int,
)
