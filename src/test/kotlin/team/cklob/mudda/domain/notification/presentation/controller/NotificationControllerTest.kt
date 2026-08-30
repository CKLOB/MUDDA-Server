package team.cklob.mudda.domain.notification.presentation.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import team.cklob.mudda.domain.notification.application.impl.DeleteDeviceTokenService
import team.cklob.mudda.domain.notification.application.impl.DeleteNotificationService
import team.cklob.mudda.domain.notification.application.impl.GetNotificationListService
import team.cklob.mudda.domain.notification.application.impl.ReadNotificationService
import team.cklob.mudda.domain.notification.application.impl.RegisterDeviceTokenService
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.domain.notification.presentation.response.NotificationListResponse
import team.cklob.mudda.domain.notification.presentation.response.NotificationResponse
import team.cklob.mudda.domain.notification.presentation.response.ReadNotificationResponse
import team.cklob.mudda.global.config.SecurityConfig
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import team.cklob.mudda.global.security.AccessTokenBlacklist
import team.cklob.mudda.global.security.JwtTokenProvider
import java.time.LocalDateTime

@WebMvcTest(controllers = [NotificationController::class], properties = [
	"jwt.secret=local-test-secret-must-be-at-least-32-bytes",
])
@Import(SecurityConfig::class, JwtTokenProvider::class)
class NotificationControllerTest(@Autowired private val mockMvc: MockMvc, @Autowired private val jwtTokenProvider: JwtTokenProvider) {
	@MockkBean lateinit var jpaMappingContext: JpaMetamodelMappingContext
	@MockkBean lateinit var accessTokenBlacklist: AccessTokenBlacklist
	@MockkBean lateinit var getNotificationListService: GetNotificationListService
	@MockkBean lateinit var readNotificationService: ReadNotificationService
	@MockkBean lateinit var deleteNotificationService: DeleteNotificationService
	@MockkBean lateinit var registerDeviceTokenService: RegisterDeviceTokenService
	@MockkBean lateinit var deleteDeviceTokenService: DeleteDeviceTokenService

	private val now: LocalDateTime = LocalDateTime.now()

	private fun accessTokenFor(memberId: Long): String {
		every { accessTokenBlacklist.isBlacklisted(any()) } returns false
		every { accessTokenBlacklist.isRevoked(any(), any()) } returns false
		return jwtTokenProvider.createAccessToken(memberId)
	}

	// -------- GET /api/v1/notification --------

	@Test fun `getNotifications requires authentication`() {
		mockMvc.perform(get("/api/v1/notification")).andExpect(status().isUnauthorized)
	}

	@Test fun `getNotifications returns the spec-shaped payload`() {
		val token = accessTokenFor(1L)
		every { getNotificationListService.execute(1L, null, any()) } returns NotificationListResponse(
			unreadCount = 2,
			notifications = listOf(
				NotificationResponse(
					notificationId = 10, type = NotificationType.CAPSULE_OPENED, title = "캡슐이 열렸어요",
					content = "nick님이 열었어요.", targetId = 5, targetType = NotificationTargetType.CAPSULE,
					isRead = false, createdAt = now,
				),
			),
			page = 0, size = 20, totalCount = 1,
		)

		mockMvc.perform(get("/api/v1/notification").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.unreadCount").value(2))
			.andExpect(jsonPath("$.data.notifications[0].notificationId").value(10))
			.andExpect(jsonPath("$.data.notifications[0].type").value("CAPSULE_OPENED"))
			.andExpect(jsonPath("$.data.notifications[0].targetType").value("CAPSULE"))
			.andExpect(jsonPath("$.data.notifications[0].isRead").value(false))
	}

	@Test fun `getNotifications passes the isRead filter through`() {
		val token = accessTokenFor(1L)
		every { getNotificationListService.execute(1L, false, any()) } returns
			NotificationListResponse(0, emptyList(), 0, 20, 0)

		mockMvc.perform(get("/api/v1/notification").param("isRead", "false").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)

		verify(exactly = 1) { getNotificationListService.execute(1L, false, any()) }
	}

	// -------- PATCH /api/v1/notification/read --------

	@Test fun `readNotifications returns the read count`() {
		val token = accessTokenFor(1L)
		every { readNotificationService.execute(1L, any()) } returns ReadNotificationResponse(3, now)

		mockMvc.perform(
			patch("/api/v1/notification/read").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"readAll":true,"notificationIds":[]}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.data.readCount").value(3))
	}

	@Test fun `readNotifications rejects an empty selection`() {
		val token = accessTokenFor(1L)
		every { readNotificationService.execute(1L, any()) } throws BusinessException(ErrorCode.INVALID_INPUT)

		mockMvc.perform(
			patch("/api/v1/notification/read").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"readAll":false,"notificationIds":[]}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.success").value(false))
	}

	// -------- DELETE /api/v1/notification/{id} --------

	@Test fun `deleteNotification answers 204`() {
		val token = accessTokenFor(1L)
		every { deleteNotificationService.execute(1L, 10L) } returns Unit

		mockMvc.perform(delete("/api/v1/notification/10").header("Authorization", "Bearer $token"))
			.andExpect(status().isNoContent)
	}

	@Test fun `deleteNotification answers 404 for a notification the caller does not own`() {
		val token = accessTokenFor(1L)
		every { deleteNotificationService.execute(1L, 10L) } throws BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND)

		mockMvc.perform(delete("/api/v1/notification/10").header("Authorization", "Bearer $token"))
			.andExpect(status().isNotFound)
	}

	// -------- device tokens --------

	@Test fun `registerDeviceToken requires authentication`() {
		mockMvc.perform(
			post("/api/v1/notification/tokens")
				.contentType(MediaType.APPLICATION_JSON).content("""{"token":"abc"}"""),
		).andExpect(status().isUnauthorized)
	}

	@Test fun `registerDeviceToken rejects a blank token`() {
		val token = accessTokenFor(1L)

		mockMvc.perform(
			post("/api/v1/notification/tokens").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"token":"  "}"""),
		).andExpect(status().isBadRequest)

		verify(exactly = 0) { registerDeviceTokenService.execute(any(), any()) }
	}

	@Test fun `registerDeviceToken answers 204`() {
		val token = accessTokenFor(1L)
		every { registerDeviceTokenService.execute(1L, any()) } returns Unit

		mockMvc.perform(
			post("/api/v1/notification/tokens").header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON).content("""{"token":"abc"}"""),
		).andExpect(status().isNoContent)
	}
}
