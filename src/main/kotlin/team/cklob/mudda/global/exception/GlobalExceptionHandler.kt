package team.cklob.mudda.global.exception

import org.springframework.http.ResponseEntity
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import team.cklob.mudda.global.response.ApiResponse
import org.slf4j.LoggerFactory

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException) = response(e.errorCode)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException) = response(ErrorCode.INVALID_INPUT)

    // Missing/malformed request bodies (e.g. a non-null Kotlin field omitted) fail Jackson
    // deserialization before validation even runs, and would otherwise fall through to the
    // catch-all 500 handler below.
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(e: HttpMessageNotReadableException) = response(ErrorCode.INVALID_INPUT)

    // A path/query variable that fails to convert to its declared type (e.g. a non-numeric member id)
    // would otherwise fall through to the catch-all 500 handler below.
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException) = response(ErrorCode.INVALID_INPUT)

    // A required @RequestParam that is missing (e.g. friend search's `keyword`, the request list's
    // `type`) would otherwise fall through to the catch-all 500 handler below.
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(e: MissingServletRequestParameterException) = response(ErrorCode.INVALID_INPUT)

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected exception type: {}", e.javaClass.name)
        return response(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    private fun response(errorCode: ErrorCode): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(errorCode.status).contentType(MediaType.APPLICATION_JSON).body(ApiResponse.failure(errorCode))
}
