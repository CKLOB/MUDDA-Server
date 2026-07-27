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
import team.cklob.mudda.domain.auth.application.impl.OAuthLoginService
import team.cklob.mudda.domain.auth.application.impl.ReissueService
import team.cklob.mudda.domain.auth.application.impl.SignoutService
import team.cklob.mudda.domain.auth.application.impl.SignupService
import team.cklob.mudda.domain.auth.application.impl.WithdrawService
import team.cklob.mudda.domain.auth.presentation.request.OAuthLoginRequest
import team.cklob.mudda.domain.auth.presentation.request.SignupRequest
import team.cklob.mudda.domain.auth.presentation.response.OAuthLoginResponse
import team.cklob.mudda.domain.auth.presentation.response.TokenResponse
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.global.exception.AuthException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.LoginUser

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val oauthLoginService: OAuthLoginService,
    private val signupService: SignupService,
    private val reissueService: ReissueService,
    private val signoutService: SignoutService,
    private val withdrawService: WithdrawService,
) {
    @PostMapping("/oauth/{provider}")
    fun oauthLogin(
        @PathVariable provider: OAuthProvider,
        @Valid @RequestBody request: OAuthLoginRequest,
    ): ResponseEntity<OAuthLoginResponse> = ResponseEntity.ok(oauthLoginService.execute(provider, request))

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@LoginUser memberId: Long, @Valid @RequestBody request: SignupRequest) {
        signupService.execute(memberId, request)
    }

    @PatchMapping("/reissue")
    fun reissue(@RequestHeader("refreshToken") refreshTokenHeader: String): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(reissueService.execute(extractBearerToken(refreshTokenHeader)))

    @DeleteMapping("/signout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun signout(@LoginUser memberId: Long, @RequestHeader("Authorization") authorization: String) {
        signoutService.execute(memberId, extractBearerToken(authorization))
    }

    @DeleteMapping("/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(@LoginUser memberId: Long, @RequestHeader("Authorization") authorization: String) {
        withdrawService.execute(memberId, extractBearerToken(authorization))
    }

    private fun extractBearerToken(header: String): String =
        header.takeIf { it.startsWith("Bearer ") }?.substring(7) ?: throw AuthException(ErrorCode.INVALID_TOKEN)
}
