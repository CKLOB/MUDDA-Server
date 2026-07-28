package team.cklob.mudda.domain.auth.presentation.response

data class ReissueAuthResponse(
    val accessToken: String,
    val refreshToken: String,
)
