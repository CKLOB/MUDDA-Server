package team.cklob.mudda.domain.auth.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "토큰 재발급 응답")
data class ReissueAuthResponse(
    @Schema(description = "새 액세스 토큰", example = "ey...")
    val accessToken: String,
    @Schema(description = "새 리프레시 토큰", example = "ey...")
    val refreshToken: String,
)
