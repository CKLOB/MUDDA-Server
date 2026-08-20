package team.cklob.mudda.domain.timecapsule.presentation.controller

import jakarta.validation.Valid
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
import team.cklob.mudda.domain.timecapsule.application.impl.CreateCapsuleService
import team.cklob.mudda.domain.timecapsule.application.impl.CreateGuestbookService
import team.cklob.mudda.domain.timecapsule.application.impl.DeleteCapsuleService
import team.cklob.mudda.domain.timecapsule.application.impl.DeleteGuestbookService
import team.cklob.mudda.domain.timecapsule.application.impl.GetCapsuleDetailService
import team.cklob.mudda.domain.timecapsule.application.impl.GetCapsuleListService
import team.cklob.mudda.domain.timecapsule.application.impl.GetGuestbookListService
import team.cklob.mudda.domain.timecapsule.application.impl.GetMyCapsuleListService
import team.cklob.mudda.domain.timecapsule.application.impl.GetNearbyCapsuleService
import team.cklob.mudda.domain.timecapsule.application.impl.GetReceivedCapsuleListService
import team.cklob.mudda.domain.timecapsule.application.impl.OpenCapsuleService
import team.cklob.mudda.domain.timecapsule.application.impl.UpdateGuestbookService
import team.cklob.mudda.domain.timecapsule.presentation.request.CreateCapsuleRequest
import team.cklob.mudda.domain.timecapsule.presentation.request.CreateGuestbookRequest
import team.cklob.mudda.domain.timecapsule.presentation.request.OpenCapsuleRequest
import team.cklob.mudda.domain.timecapsule.presentation.request.UpdateGuestbookRequest
import team.cklob.mudda.domain.timecapsule.presentation.response.CreateCapsuleResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.GuestbookResponse
import team.cklob.mudda.global.response.ApiResponse
import team.cklob.mudda.global.security.LoginUser

@RestController
@RequestMapping("/api/v1/capsule")
class CapsuleController(
	private val createCapsuleService: CreateCapsuleService,
	private val getCapsuleListService: GetCapsuleListService,
	private val getNearbyCapsuleService: GetNearbyCapsuleService,
	private val getCapsuleDetailService: GetCapsuleDetailService,
	private val getMyCapsuleListService: GetMyCapsuleListService,
	private val getReceivedCapsuleListService: GetReceivedCapsuleListService,
	private val openCapsuleService: OpenCapsuleService,
	private val deleteCapsuleService: DeleteCapsuleService,
	private val createGuestbookService: CreateGuestbookService,
	private val getGuestbookListService: GetGuestbookListService,
	private val updateGuestbookService: UpdateGuestbookService,
	private val deleteGuestbookService: DeleteGuestbookService,
) {
	@PostMapping
	fun create(@LoginUser memberId: Long, @Valid @RequestBody request: CreateCapsuleRequest): ResponseEntity<ApiResponse<CreateCapsuleResponse>> =
		ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createCapsuleService.execute(memberId, request)))

	@GetMapping
	fun list(@LoginUser memberId: Long, @RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) =
		ApiResponse.success(getCapsuleListService.execute(memberId, page, size))

	@GetMapping("/nearby")
	fun nearby(
		@LoginUser memberId: Long,
		@RequestParam latitude: Double,
		@RequestParam longitude: Double,
		@RequestParam radius: Double,
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "20") size: Int,
	) = ApiResponse.success(getNearbyCapsuleService.execute(memberId, latitude, longitude, radius, page, size))

	@GetMapping("/me")
	fun mine(@LoginUser memberId: Long, @RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) =
		ApiResponse.success(getMyCapsuleListService.execute(memberId, page, size))

	@GetMapping("/received")
	fun received(@LoginUser memberId: Long, @RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) =
		ApiResponse.success(getReceivedCapsuleListService.execute(memberId, page, size))

	@GetMapping("/{capsuleId}")
	fun detail(@LoginUser memberId: Long, @PathVariable capsuleId: Long) =
		ApiResponse.success(getCapsuleDetailService.execute(memberId, capsuleId))

	@PostMapping("/{capsuleId}/open")
	fun open(@LoginUser memberId: Long, @PathVariable capsuleId: Long, @Valid @RequestBody request: OpenCapsuleRequest) =
		ApiResponse.success(openCapsuleService.execute(memberId, capsuleId, request))

	@DeleteMapping("/{capsuleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@LoginUser memberId: Long, @PathVariable capsuleId: Long) = deleteCapsuleService.execute(memberId, capsuleId)

	@PostMapping("/{capsuleId}/guestbooks")
	fun createGuestbook(@LoginUser memberId: Long, @PathVariable capsuleId: Long, @Valid @RequestBody request: CreateGuestbookRequest): ResponseEntity<ApiResponse<GuestbookResponse>> =
		ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createGuestbookService.execute(memberId, capsuleId, request)))

	@GetMapping("/{capsuleId}/guestbooks")
	fun guestbooks(@LoginUser memberId: Long, @PathVariable capsuleId: Long, @RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) =
		ApiResponse.success(getGuestbookListService.execute(memberId, capsuleId, page, size))

	@PatchMapping("/{capsuleId}/guestbooks/{guestbookId}")
	fun updateGuestbook(@LoginUser memberId: Long, @PathVariable capsuleId: Long, @PathVariable guestbookId: Long, @Valid @RequestBody request: UpdateGuestbookRequest) =
		ApiResponse.success(updateGuestbookService.execute(memberId, capsuleId, guestbookId, request))

	@DeleteMapping("/{capsuleId}/guestbooks/{guestbookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun deleteGuestbook(@LoginUser memberId: Long, @PathVariable capsuleId: Long, @PathVariable guestbookId: Long) =
		deleteGuestbookService.execute(memberId, capsuleId, guestbookId)
}
