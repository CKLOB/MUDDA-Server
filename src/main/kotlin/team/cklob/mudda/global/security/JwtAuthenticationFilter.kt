package team.cklob.mudda.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import team.cklob.mudda.global.util.BearerToken

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val accessTokenBlacklist: AccessTokenBlacklist,
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        BearerToken.extract(request.getHeader("Authorization"))
            ?.let(jwtTokenProvider::parseAccessToken)
            ?.takeUnless { claims -> claims.id?.let(accessTokenBlacklist::isBlacklisted) ?: false }
            ?.takeUnless { claims -> accessTokenBlacklist.isRevoked(claims.subject.toLong(), claims.issuedAt.toInstant()) }
            ?.let { claims ->
                val authentication = UsernamePasswordAuthenticationToken(claims.subject.toLong(), null, emptyList())
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        filterChain.doFilter(request, response)
    }
}
