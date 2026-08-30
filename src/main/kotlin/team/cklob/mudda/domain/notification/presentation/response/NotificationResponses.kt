package team.cklob.mudda.domain.notification.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.notification.domain.entity.Notification
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import java.time.LocalDateTime

@Schema(description = "알림 단건")
data class NotificationResponse(
	@Schema(description = "알림 ID", example = "1")
	val notificationId: Long,

	@Schema(description = "알림 종류", example = "CAPSULE_OPENED")
	val type: NotificationType,

	@Schema(description = "알림 제목", example = "캡슐이 열렸어요")
	val title: String,

	@Schema(description = "알림 본문", example = "nick님이 회고 캡슐을 열었어요.")
	val content: String,

	@Schema(description = "알림을 눌렀을 때 이동할 대상 ID", example = "12", nullable = true)
	val targetId: Long?,

	@Schema(description = "대상 종류", example = "CAPSULE", nullable = true)
	val targetType: NotificationTargetType?,

	@Schema(description = "읽음 여부", example = "false")
	val isRead: Boolean,

	@Schema(description = "생성 시각")
	val createdAt: LocalDateTime,
) {
	companion object {
		fun from(notification: Notification) = NotificationResponse(
			notificationId = requireNotNull(notification.id),
			type = notification.notificationType,
			title = notification.title,
			content = notification.content,
			targetId = notification.targetId,
			targetType = notification.targetType,
			isRead = notification.isRead,
			createdAt = notification.createdAt,
		)
	}
}

@Schema(description = "알림 목록 응답")
data class NotificationListResponse(
	@Schema(description = "읽지 않은 알림 수. 조회 필터와 무관하게 항상 전체 미읽음 수를 반환합니다.", example = "3")
	val unreadCount: Long,

	@Schema(description = "알림 목록")
	val notifications: List<NotificationResponse>,

	@Schema(description = "현재 페이지 번호(0-base)", example = "0")
	val page: Int,

	@Schema(description = "페이지 크기", example = "20")
	val size: Int,

	@Schema(description = "조회 조건에 해당하는 전체 알림 수", example = "42")
	val totalCount: Long,
)

@Schema(description = "알림 읽음 처리 응답")
data class ReadNotificationResponse(
	@Schema(description = "이번 요청으로 읽음 처리된 알림 수. 이미 읽은 알림은 세지 않습니다.", example = "3")
	val readCount: Int,

	@Schema(description = "읽음 처리 시각")
	val updatedAt: LocalDateTime,
)
