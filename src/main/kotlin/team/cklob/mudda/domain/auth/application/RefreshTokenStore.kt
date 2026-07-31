package team.cklob.mudda.domain.auth.application

interface RefreshTokenStore {
    fun save(memberId: Long, refreshToken: String)
    fun find(memberId: Long): String?
    fun delete(memberId: Long)
}
