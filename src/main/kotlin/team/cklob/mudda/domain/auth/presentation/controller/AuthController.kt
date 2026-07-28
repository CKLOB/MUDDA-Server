package team.cklob.mudda.domain.auth.presentation.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.cklob.mudda.domain.auth.application.impl.LoginAuthService
import team.cklob.mudda.domain.auth.application.impl.ReissueAuthService
import team.cklob.mudda.domain.auth.application.impl.SignoutAuthService
import team.cklob.mudda.domain.auth.application.impl.SignupAuthService
import team.cklob.mudda.domain.auth.application.impl.WithdrawAuthService
import team.cklob.mudda.domain.auth.presentation.request.LoginAuthRequest
import team.cklob.mudda.domain.auth.presentation.request.SignupAuthRequest
import team.cklob.mudda.domain.auth.presentation.response.LoginAuthResponse
import team.cklob.mudda.domain.auth.presentation.response.ReissueAuthResponse
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.LoginUser

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val loginAuthService: LoginAuthService,
    private val signupAuthService: SignupAuthService,
    private val reissueAuthService: ReissueAuthService,
    private val signoutAuthService: SignoutAuthService,
    private val withdrawAuthService: WithdrawAuthService,
) {
    @PostMapping("/oauth/{provider}")
    fun oauthLogin(
        @PathVariable provider: OAuthProvider,
        @Valid @RequestBody request: LoginAuthRequest,
    ): ResponseEntity<LoginAuthResponse> = ResponseEntity.ok(loginAuthService.execute(provider, request))

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@LoginUser memberId: Long, @Valid @RequestBody request: SignupAuthRequest) {
        signupAuthService.execute(memberId, request)
    }

    @PatchMapping("/reissue")
    fun reissue(@RequestHeader("refreshToken") refreshTokenHeader: String): ResponseEntity<ReissueAuthResponse> =
        ResponseEntity.ok(reissueAuthService.execute(extractBearerToken(refreshTokenHeader)))

    @DeleteMapping("/signout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun signout(@LoginUser memberId: Long, @RequestHeader("Authorization") authorization: String) {
        signoutAuthService.execute(memberId, extractBearerToken(authorization))
    }

    @DeleteMapping("/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(@LoginUser memberId: Long, @RequestHeader("Authorization") authorization: String) {
        withdrawAuthService.execute(memberId, extractBearerToken(authorization))
    }

    private fun extractBearerToken(header: String): String =
        header.takeIf { it.startsWith("Bearer ") }?.substring(7) ?: throw AuthException(ErrorCode.INVALID_TOKEN)
}
