package team.cklob.mudda.domain.notification.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "알림 읽음 처리 요청")
data class ReadNotificationRequest(
	@Schema(description = "true면 로그인 사용자의 모든 미읽음 알림을 읽음 처리하고 notificationIds는 무시합니다.", example = "false")
	val readAll: Boolean = false,

	@Schema(description = "읽음 처리할 알림 ID 목록. readAll이 false일 때 사용합니다.", example = "[1, 2, 3]")
	val notificationIds: List<Long> = emptyList(),
)

@Schema(description = "디바이스 토큰 등록 요청")
data class RegisterDeviceTokenRequest(
	@field:NotBlank
	@field:Size(max = 255)
	@Schema(description = "FCM 등록 토큰", example = "cJk8...")
	val token: String,
)
