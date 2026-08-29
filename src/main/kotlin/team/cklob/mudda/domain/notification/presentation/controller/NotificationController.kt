package team.cklob.mudda.domain.notification.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.cklob.mudda.domain.notification.application.impl.DeleteDeviceTokenService
import team.cklob.mudda.domain.notification.application.impl.DeleteNotificationService
import team.cklob.mudda.domain.notification.application.impl.GetNotificationListService
import team.cklob.mudda.domain.notification.application.impl.ReadNotificationService
import team.cklob.mudda.domain.notification.application.impl.RegisterDeviceTokenService
import team.cklob.mudda.domain.notification.presentation.request.ReadNotificationRequest
import team.cklob.mudda.domain.notification.presentation.request.RegisterDeviceTokenRequest
import team.cklob.mudda.domain.notification.presentation.response.NotificationListResponse
import team.cklob.mudda.domain.notification.presentation.response.ReadNotificationResponse
import team.cklob.mudda.global.response.ApiResponse
import team.cklob.mudda.global.security.LoginUser

@Tag(name = "Notification", description = "알림 조회/읽음/삭제 및 디바이스 토큰 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notification")
class NotificationController(
	private val getNotificationListService: GetNotificationListService,
	private val readNotificationService: ReadNotificationService,
	private val deleteNotificationService: DeleteNotificationService,
	private val registerDeviceTokenService: RegisterDeviceTokenService,
	private val deleteDeviceTokenService: DeleteDeviceTokenService,
) {
	@Operation(summary = "알림 목록 조회", description = "로그인 사용자의 알림을 최신순으로 조회합니다. isRead를 생략하면 읽음/미읽음을 모두 반환합니다.")
	@GetMapping
	fun getNotifications(
		@LoginUser memberId: Long,
		@Parameter(description = "읽음 여부 필터. 생략 시 전체") @RequestParam(required = false) isRead: Boolean?,
		@PageableDefault(size = 20) pageable: Pageable,
	): ResponseEntity<ApiResponse<NotificationListResponse>> =
		ResponseEntity.ok(ApiResponse.success(getNotificationListService.execute(memberId, isRead, pageable)))

	@Operation(summary = "알림 읽음 처리", description = "readAll이 true면 전체를, 아니면 notificationIds에 담긴 알림만 읽음 처리합니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "200", description = "처리 성공"),
		SwaggerApiResponse(responseCode = "400", description = "readAll이 false인데 notificationIds가 비어 있음"),
	)
	@PatchMapping("/read")
	fun readNotifications(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: ReadNotificationRequest,
	): ResponseEntity<ApiResponse<ReadNotificationResponse>> =
		ResponseEntity.ok(ApiResponse.success(readNotificationService.execute(memberId, request)))

	@Operation(summary = "알림 삭제", description = "로그인 사용자가 수신한 알림을 삭제합니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "204", description = "삭제 성공"),
		SwaggerApiResponse(responseCode = "404", description = "알림 없음 또는 본인 알림이 아님(NOTIFICATION_NOT_FOUND)"),
	)
	@DeleteMapping("/{notificationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun deleteNotification(@LoginUser memberId: Long, @PathVariable notificationId: Long) {
		deleteNotificationService.execute(memberId, notificationId)
	}

	// Not in the API spec: FCM cannot deliver a push without a per-device registration token, and the spec
	// has no endpoint for the client to hand one over. Added here rather than invented elsewhere.
	@Operation(summary = "디바이스 토큰 등록", description = "푸시 수신용 FCM 토큰을 등록합니다. 이미 등록된 토큰이면 소유자를 로그인 사용자로 갱신합니다.")
	@PostMapping("/tokens")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun registerDeviceToken(@LoginUser memberId: Long, @Valid @RequestBody request: RegisterDeviceTokenRequest) {
		registerDeviceTokenService.execute(memberId, request)
	}

	@Operation(summary = "디바이스 토큰 삭제", description = "로그아웃 등으로 더 이상 푸시를 받지 않을 토큰을 제거합니다.")
	@DeleteMapping("/tokens")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun deleteDeviceToken(@LoginUser memberId: Long, @RequestParam token: String) {
		deleteDeviceTokenService.execute(memberId, token)
	}
}
