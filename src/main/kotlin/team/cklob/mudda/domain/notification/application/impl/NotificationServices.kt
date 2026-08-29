package team.cklob.mudda.domain.notification.application.impl

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.notification.domain.repository.DeviceTokenRepository
import team.cklob.mudda.domain.notification.domain.entity.DeviceToken
import team.cklob.mudda.domain.notification.domain.repository.NotificationRepository
import team.cklob.mudda.domain.notification.presentation.request.ReadNotificationRequest
import team.cklob.mudda.domain.notification.presentation.request.RegisterDeviceTokenRequest
import team.cklob.mudda.domain.notification.presentation.response.NotificationListResponse
import team.cklob.mudda.domain.notification.presentation.response.NotificationResponse
import team.cklob.mudda.domain.notification.presentation.response.ReadNotificationResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime

@Service
class GetNotificationListService(
	private val notificationRepository: NotificationRepository,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, isRead: Boolean?, pageable: Pageable): NotificationListResponse {
		val page = if (isRead == null) {
			notificationRepository.findByRecipientIdOrderByCreatedAtDesc(memberId, pageable)
		} else {
			notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(memberId, isRead, pageable)
		}
		return NotificationListResponse(
			// Reported independently of the isRead filter: the client uses it for the badge count, which
			// must not change just because the user is looking at the "read" tab.
			unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(memberId),
			notifications = page.content.map(NotificationResponse::from),
			page = page.number,
			size = page.size,
			totalCount = page.totalElements,
		)
	}
}

@Service
class ReadNotificationService(
	private val notificationRepository: NotificationRepository,
) {
	@Transactional
	fun execute(memberId: Long, request: ReadNotificationRequest): ReadNotificationResponse {
		// The bulk updates are scoped by recipient id, so ids belonging to someone else simply match no
		// rows -- a caller cannot mark another member's notifications read, and probing for existence
		// yields nothing either way.
		val readCount = when {
			request.readAll -> notificationRepository.markAllRead(memberId)
			request.notificationIds.isEmpty() -> throw BusinessException(ErrorCode.INVALID_INPUT)
			else -> notificationRepository.markRead(memberId, request.notificationIds)
		}
		return ReadNotificationResponse(readCount, LocalDateTime.now())
	}
}

@Service
class DeleteNotificationService(
	private val notificationRepository: NotificationRepository,
) {
	@Transactional
	fun execute(memberId: Long, notificationId: Long) {
		val notification = notificationRepository.findById(notificationId)
			.orElseThrow { BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND) }
		// Someone else's notification is reported as not-found rather than forbidden: the id space is
		// global, and a 403 would confirm that a given id exists for another member.
		if (notification.recipient.id != memberId) throw BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND)
		notificationRepository.delete(notification)
	}
}

@Service
class RegisterDeviceTokenService(
	private val deviceTokenRepository: DeviceTokenRepository,
	private val memberRepository: MemberRepository,
) {
	@Transactional
	fun execute(memberId: Long, request: RegisterDeviceTokenRequest) {
		val existing = deviceTokenRepository.findByToken(request.token).orElse(null)
		if (existing != null) {
			// Same physical device, different account (a re-login or a handover). FCM keeps the token, so
			// re-point the row instead of inserting a duplicate that would push to the wrong user.
			if (existing.member.id != memberId) {
				existing.member = memberRepository.findById(memberId)
					.orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
			}
			return
		}
		val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		deviceTokenRepository.save(DeviceToken(member, request.token))
	}
}

@Service
class DeleteDeviceTokenService(
	private val deviceTokenRepository: DeviceTokenRepository,
) {
	@Transactional
	fun execute(memberId: Long, token: String) {
		// Scoped by owner so one member cannot unregister another member's device.
		deviceTokenRepository.findByToken(token)
			.filter { it.member.id == memberId }
			.ifPresent(deviceTokenRepository::delete)
	}
}
