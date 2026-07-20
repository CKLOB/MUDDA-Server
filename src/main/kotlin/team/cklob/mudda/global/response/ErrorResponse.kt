package team.cklob.mudda.global.response

import team.cklob.mudda.global.exception.ErrorCode

data class ErrorResponse(val code: String, val message: String) {
    constructor(errorCode: ErrorCode) : this(errorCode.code, errorCode.message)
}
