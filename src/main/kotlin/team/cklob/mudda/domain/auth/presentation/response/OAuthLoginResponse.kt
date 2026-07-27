package team.cklob.mudda.domain.auth.presentation.response

data class OAuthLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewMember: Boolean,
)
