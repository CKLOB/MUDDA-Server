package team.cklob.mudda.global.util

object BearerToken {
    private const val PREFIX = "Bearer "

    fun extract(header: String?): String? = header?.takeIf { it.startsWith(PREFIX) }?.substring(PREFIX.length)
}
