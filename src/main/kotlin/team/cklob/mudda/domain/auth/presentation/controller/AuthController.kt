package team.cklob.mudda.domain.auth.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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
import team.cklob.mudda.global.response.ApiResponse
import team.cklob.mudda.global.security.LoginUser
import team.cklob.mudda.global.util.BearerToken

@Tag(name = "Auth", description = "OAuth 로그인, 회원가입, 토큰 재발급, 로그아웃, 탈퇴 API")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val loginAuthService: LoginAuthService,
    private val signupAuthService: SignupAuthService,
    private val reissueAuthService: ReissueAuthService,
    private val signoutAuthService: SignoutAuthService,
    private val withdrawAuthService: WithdrawAuthService,
) {
    @Operation(
        summary = "OAuth 로그인",
        description = "소셜 로그인 인가 코드로 로그인합니다. 인증이 필요 없는 엔드포인트입니다. " +
            "최초 로그인이면 회원가입이 완료되지 않은 상태의 토큰이 발급되며, 이어서 `/signup` 을 호출해야 합니다.",
    )
    @SwaggerApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
        SwaggerApiResponse(responseCode = "400", description = "유효하지 않은 인가 코드(OAUTH_INVALID_CODE) 또는 지원하지 않는 제공자(OAUTH_PROVIDER_NOT_SUPPORTED)"),
        SwaggerApiResponse(responseCode = "403", description = "탈퇴한 계정(WITHDRAWN_MEMBER)"),
    )
    @PostMapping("/oauth/{provider}")
    fun oauthLogin(
        @Parameter(description = "OAuth 제공자", example = "KAKAO") @PathVariable provider: OAuthProvider,
        @Valid @RequestBody request: LoginAuthRequest,
    ): ResponseEntity<ApiResponse<LoginAuthResponse>> = ResponseEntity.ok(ApiResponse.success(loginAuthService.execute(provider, request)))

    @Operation(summary = "회원가입", description = "OAuth 로그인 직후 닉네임 등 프로필 정보를 등록해 회원가입을 완료합니다.")
    @SwaggerApiResponses(
        SwaggerApiResponse(responseCode = "201", description = "회원가입 성공"),
        SwaggerApiResponse(responseCode = "409", description = "이미 회원가입을 마친 회원(ALREADY_SIGNED_UP) 또는 닉네임 중복(NICKNAME_ALREADY_EXISTS)"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(@LoginUser memberId: Long, @Valid @RequestBody request: SignupAuthRequest) {
        signupAuthService.execute(memberId, request)
    }

    @Operation(
        summary = "토큰 재발급",
        description = "리프레시 토큰으로 액세스 토큰을 재발급합니다. 인증이 필요 없는 엔드포인트이며, 리프레시 토큰은 `refreshToken` 헤더로 전달합니다.",
    )
    @SwaggerApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "재발급 성공"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 리프레시 토큰(INVALID_REFRESH_TOKEN)"),
    )
    @PatchMapping("/reissue")
    fun reissue(
        @Parameter(description = "리프레시 토큰. `Bearer ` 접두사를 포함합니다.", example = "Bearer ey...")
        @RequestHeader("refreshToken") refreshTokenHeader: String,
    ): ResponseEntity<ApiResponse<ReissueAuthResponse>> =
        ResponseEntity.ok(ApiResponse.success(reissueAuthService.execute(extractBearerToken(refreshTokenHeader))))

    @Operation(summary = "로그아웃", description = "현재 액세스 토큰을 블랙리스트에 등록하고 리프레시 토큰을 폐기합니다.")
    @SwaggerApiResponses(
        SwaggerApiResponse(responseCode = "204", description = "로그아웃 성공"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않은 토큰(INVALID_TOKEN)"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/signout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun signout(@LoginUser memberId: Long, @RequestHeader("Authorization") authorization: String) {
        signoutAuthService.execute(memberId, extractBearerToken(authorization))
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "회원을 탈퇴 처리합니다. 같은 소셜 계정으로 다시 가입할 수 있도록 기존 행은 tombstone 처리됩니다.",
    )
    @SwaggerApiResponses(
        SwaggerApiResponse(responseCode = "204", description = "탈퇴 성공"),
        SwaggerApiResponse(responseCode = "401", description = "유효하지 않은 토큰(INVALID_TOKEN)"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(@LoginUser memberId: Long, @RequestHeader("Authorization") authorization: String) {
        withdrawAuthService.execute(memberId, extractBearerToken(authorization))
    }

    private fun extractBearerToken(header: String): String = BearerToken.extract(header) ?: throw AuthException(ErrorCode.INVALID_TOKEN)
}
