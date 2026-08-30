package team.cklob.mudda.domain.auth.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "OAuth 로그인 요청")
data class LoginAuthRequest(
    @field:NotBlank
    @Schema(description = "OAuth 제공자로부터 받은 인가 코드", example = "abc123")
    val code: String,

    @field:NotBlank
    @Schema(description = "인가 코드를 발급받을 때 사용한 리다이렉트 URI", example = "https://mudda.app/oauth/callback")
    val redirectUri: String,
)
