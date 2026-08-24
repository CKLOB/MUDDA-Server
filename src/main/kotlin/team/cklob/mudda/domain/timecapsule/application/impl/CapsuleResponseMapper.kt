package team.cklob.mudda.domain.timecapsule.application.impl

import team.cklob.mudda.domain.timecapsule.domain.entity.Guestbook
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.presentation.response.CapsuleDetailResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.CapsuleListItemResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.GuestbookResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.WriterResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

internal fun writer(capsule: TimeCapsule) = WriterResponse(
	memberId = requireNotNull(capsule.member.id),
	nickname = capsule.member.nickname,
	profileImageUrl = capsule.member.profileImageUrl,
)

internal fun TimeCapsule.toListItem(isOpened: Boolean, includeWriter: Boolean = true) = CapsuleListItemResponse(
	capsuleId = requireNotNull(id),
	title = name,
	writer = writer(this).takeIf { includeWriter },
	latitude = location.y,
	longitude = location.x,
	openAt = openAt,
	expiredAt = expiredAt,
	visibility = visibility,
	requiresPassword = lockType == CapsuleLockType.PASSWORD,
	requiresAnswer = lockType == CapsuleLockType.QUESTION,
	isOpened = isOpened,
	createdAt = createdAt,
)

internal fun TimeCapsule.toDetail(isOpened: Boolean) = CapsuleDetailResponse(
	capsuleId = requireNotNull(id),
	title = name,
	writer = writer(this),
	latitude = location.y,
	longitude = location.x,
	requiredDistance = openRadiusMeter.toDouble(),
	openAt = openAt,
	expiredAt = expiredAt,
	visibility = visibility,
	lockType = lockType,
	question = question,
	isOpened = isOpened,
	createdAt = createdAt,
	updatedAt = updatedAt,
)

internal fun Guestbook.toResponse() = GuestbookResponse(
	guestbookId = requireNotNull(id),
	capsuleId = requireNotNull(timeCapsule.id),
	writer = WriterResponse(requireNotNull(member.id), member.nickname, member.profileImageUrl),
	content = content,
	createdAt = createdAt,
	updatedAt = updatedAt,
)

internal fun <T> page(items: List<T>, page: Int, size: Int): Triple<List<T>, Int, Long> {
	if (page < 0 || size !in 1..50) throw BusinessException(ErrorCode.INVALID_INPUT)
	val from = (page.toLong() * size).coerceAtMost(items.size.toLong()).toInt()
	val to = (from + size).coerceAtMost(items.size)
	return Triple(items.subList(from, to), page, items.size.toLong())
}
