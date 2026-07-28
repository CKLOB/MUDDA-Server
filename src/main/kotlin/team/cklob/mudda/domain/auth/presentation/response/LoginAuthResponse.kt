package team.cklob.mudda.domain.auth.presentation.response

data class LoginAuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewMember: Boolean,
)
