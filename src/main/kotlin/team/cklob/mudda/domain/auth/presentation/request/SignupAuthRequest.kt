package team.cklob.mudda.domain.auth.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import team.cklob.mudda.domain.member.domain.type.Gender

@Schema(description = "회원가입 요청")
data class SignupAuthRequest(
    @field:NotBlank
    @field:Size(max = 30)
    @Schema(description = "실명", example = "박하민")
    val name: String,

    @field:NotBlank
    @field:Size(max = 30)
    @Schema(description = "닉네임. 전체에서 유일해야 합니다.", example = "hamin")
    val nickname: String,

    @Schema(description = "성별", example = "MALE")
    val gender: Gender,

    @field:Min(1900)
    @field:Max(2100)
    @Schema(description = "출생 연도", example = "2008")
    val birthYear: Int,
)
