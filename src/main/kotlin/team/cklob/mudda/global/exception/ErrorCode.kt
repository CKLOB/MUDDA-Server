package team.cklob.mudda.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(val status: HttpStatus, val code: String, val message: String) {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "Invalid input."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "Internal server error."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Authentication is required."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "Invalid token."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Invalid or expired refresh token."),
    OAUTH_INVALID_CODE(HttpStatus.BAD_REQUEST, "A004", "Invalid OAuth authorization code."),
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "A006", "This OAuth provider is not supported yet."),
    WITHDRAWN_MEMBER(HttpStatus.FORBIDDEN, "A007", "This account has been withdrawn."),
    ALREADY_SIGNED_UP(HttpStatus.CONFLICT, "A008", "This member has already completed signup."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "M001", "Nickname already exists."),
    CAPSULE_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "Time capsule not found."),
}
