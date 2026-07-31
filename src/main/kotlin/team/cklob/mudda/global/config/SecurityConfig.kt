package team.cklob.mudda.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.http.HttpStatus
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtAuthenticationFilter
import team.cklob.mudda.global.security.JwtProperties
import team.cklob.mudda.global.security.JwtTokenProvider
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.response.ApiResponse

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(private val objectMapper: ObjectMapper) {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtTokenProvider: JwtTokenProvider,
        accessTokenBlacklist: AccessTokenBlacklist,
    ): SecurityFilterChain = http
        .csrf { it.disable() }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests { it.requestMatchers("/api/v1/auth/oauth/**", "/api/v1/auth/reissue", "/api/v1/maps/**", "/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll().anyRequest().authenticated() }
        .exceptionHandling { it.authenticationEntryPoint { _, response, _ ->
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = "application/json"
            response.writer.write(objectMapper.writeValueAsString(ApiResponse.failure(ErrorCode.UNAUTHORIZED)))
        } }
        .addFilterBefore(JwtAuthenticationFilter(jwtTokenProvider, accessTokenBlacklist), UsernamePasswordAuthenticationFilter::class.java).build()
}
