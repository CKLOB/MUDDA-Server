package team.cklob.mudda.domain.auth.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "OAuth 로그인 응답")
data class LoginAuthResponse(
    @Schema(description = "액세스 토큰", example = "ey...")
    val accessToken: String,
    @Schema(description = "리프레시 토큰", example = "ey...")
    val refreshToken: String,
    @Schema(description = "true면 아직 회원가입이 완료되지 않은 상태이므로 이어서 /api/v1/auth/signup 을 호출해야 합니다.", example = "true")
    val isNewMember: Boolean,
)
