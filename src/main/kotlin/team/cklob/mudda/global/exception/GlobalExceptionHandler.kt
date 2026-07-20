package team.cklob.mudda.global.exception

import org.springframework.http.ResponseEntity
import org.springframework.http.MediaType
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import team.cklob.mudda.global.response.ApiResponse
import org.slf4j.LoggerFactory

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException) = response(e.errorCode)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException) = response(ErrorCode.INVALID_INPUT)

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected exception type: {}", e.javaClass.name)
        return response(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    private fun response(errorCode: ErrorCode): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(errorCode.status).contentType(MediaType.APPLICATION_JSON).body(ApiResponse.failure(errorCode))
}
