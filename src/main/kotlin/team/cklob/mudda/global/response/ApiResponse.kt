package team.cklob.mudda.global.response

import com.fasterxml.jackson.annotation.JsonInclude
import team.cklob.mudda.global.exception.ErrorCode

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(val success: Boolean, val data: T? = null, val error: ErrorResponse? = null) {
    companion object {
        fun <T> success(data: T? = null) = ApiResponse(success = true, data = data)
        fun failure(errorCode: ErrorCode) = ApiResponse<Nothing>(success = false, error = ErrorResponse(errorCode))
    }
}
