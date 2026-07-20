package team.cklob.mudda.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(val status: HttpStatus, val code: String, val message: String) {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "Invalid input."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "Internal server error."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Authentication is required."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "Invalid token."),
    CAPSULE_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "Time capsule not found."),
}
