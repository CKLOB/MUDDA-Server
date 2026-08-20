package team.cklob.mudda.domain.timecapsule.presentation.response

import team.cklob.mudda.domain.media.domain.type.MediaType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import java.time.LocalDateTime

data class WriterResponse(val memberId: Long, val nickname: String?, val profileImageUrl: String?)
data class MediaResponse(val mediaId: Long, val url: String, val type: MediaType)

data class CreateCapsuleResponse(
	val capsuleId: Long,
	val title: String,
	val latitude: Double,
	val longitude: Double,
	val openAt: LocalDateTime,
	val expiredAt: LocalDateTime?,
	val createdAt: LocalDateTime,
)

data class CapsuleListItemResponse(
	val capsuleId: Long,
	val title: String,
	val writer: WriterResponse?,
	val latitude: Double,
	val longitude: Double,
	val openAt: LocalDateTime,
	val expiredAt: LocalDateTime?,
	val visibility: CapsuleVisibility,
	val requiresPassword: Boolean,
	val requiresAnswer: Boolean,
	val isOpened: Boolean,
	val createdAt: LocalDateTime,
)

data class NearbyCapsuleResponse(
	val capsuleId: Long,
	val title: String,
	val latitude: Double,
	val longitude: Double,
	val distance: Double,
	val requiredDistance: Double,
	val openAt: LocalDateTime,
	val lockType: CapsuleLockType,
	val isOpened: Boolean,
)

data class CapsuleDetailResponse(
	val capsuleId: Long,
	val title: String,
	val writer: WriterResponse,
	val latitude: Double,
	val longitude: Double,
	val requiredDistance: Double,
	val openAt: LocalDateTime,
	val expiredAt: LocalDateTime?,
	val visibility: CapsuleVisibility,
	val lockType: CapsuleLockType,
	val question: String?,
	val isOpened: Boolean,
	val createdAt: LocalDateTime,
	val updatedAt: LocalDateTime,
)

data class OpenCapsuleResponse(
	val capsuleId: Long,
	val title: String,
	val content: String,
	val writer: WriterResponse,
	val media: List<MediaResponse>,
	val openedAt: LocalDateTime,
)

data class CapsulePageResponse<T>(val capsules: List<T>, val page: Int, val size: Int, val totalCount: Long)

data class GuestbookResponse(
	val guestbookId: Long,
	val capsuleId: Long,
	val writer: WriterResponse,
	val content: String,
	val createdAt: LocalDateTime,
	val updatedAt: LocalDateTime,
)

data class GuestbookPageResponse(val guestbooks: List<GuestbookResponse>, val page: Int, val size: Int, val totalCount: Long)
data class UpdateGuestbookResponse(val guestbookId: Long, val content: String, val updatedAt: LocalDateTime)
