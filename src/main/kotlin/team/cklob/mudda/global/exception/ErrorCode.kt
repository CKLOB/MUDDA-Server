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
    SIGNUP_REQUIRED(HttpStatus.FORBIDDEN, "A009", "Signup must be completed before this action."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "M001", "Nickname already exists."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "Member not found."),
    PROFILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "M003", "You do not have access to this profile."),
    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "Media not found."),
    INVALID_MEDIA_UPLOAD(HttpStatus.BAD_REQUEST, "D002", "Invalid media upload."),
    MEDIA_ALREADY_ATTACHED(HttpStatus.CONFLICT, "D003", "Attached media cannot be deleted."),
    MEDIA_STORAGE_ERROR(HttpStatus.BAD_GATEWAY, "D004", "Media storage request failed."),
    CAPSULE_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "Time capsule not found."),
    CANNOT_REQUEST_SELF(HttpStatus.BAD_REQUEST, "F001", "Cannot send a friend request to yourself."),
    FRIEND_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "F002", "A pending friend request already exists."),
    ALREADY_FRIENDS(HttpStatus.CONFLICT, "F003", "You are already friends with this member."),
    REVERSE_FRIEND_REQUEST_EXISTS(HttpStatus.CONFLICT, "F004", "This member has already sent you a friend request."),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "F005", "Friend request not found."),
    FRIEND_REQUEST_NOT_RECEIVER(HttpStatus.FORBIDDEN, "F006", "Only the request recipient can respond to it."),
    FRIEND_REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "F007", "This friend request has already been processed."),
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "F008", "Friend relationship not found."),
    BLOCKED_MEMBER(HttpStatus.FORBIDDEN, "F009", "This action is not allowed due to a block relationship."),
    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "F010", "Search keyword must not be blank."),
}
