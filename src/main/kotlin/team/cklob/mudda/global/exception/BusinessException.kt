package team.cklob.mudda.global.exception

open class BusinessException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)
class AuthException(errorCode: ErrorCode = ErrorCode.UNAUTHORIZED) : BusinessException(errorCode)
class CapsuleException(errorCode: ErrorCode = ErrorCode.CAPSULE_NOT_FOUND) : BusinessException(errorCode)
