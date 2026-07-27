package team.cklob.mudda.domain.auth.presentation.response

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
