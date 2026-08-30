package team.cklob.mudda.domain.timecapsule.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "Capsule", description = "타임캡슐 생성·조회·열람·삭제 및 방명록 API")
@SecurityRequirement(name = "bearerAuth")
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
	@Operation(
		summary = "타임캡슐 묻기",
		description = "지정한 좌표에 캡슐을 묻습니다. 수신자는 친구여야 하며, 첨부 미디어는 본인이 업로드했고 아직 다른 캡슐에 붙지 않은 것이어야 합니다. " +
			"잠금 유형에 따라 password 또는 question/answer 조합이 필요합니다.",
	)
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "201", description = "생성 성공"),
		SwaggerApiResponse(responseCode = "400", description = "잠금 조합·공개 시각·만료 시각이 유효하지 않거나, 수신자(INVALID_CAPSULE_RECIPIENT)·미디어(INVALID_CAPSULE_MEDIA)가 부적합"),
		SwaggerApiResponse(responseCode = "409", description = "활성 캡슐 개수 한도 초과(CAPSULE_LIMIT_EXCEEDED)"),
	)
	@PostMapping
	fun create(@LoginUser memberId: Long, @Valid @RequestBody request: CreateCapsuleRequest): ResponseEntity<ApiResponse<CreateCapsuleResponse>> =
		ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createCapsuleService.execute(memberId, request)))

	@Operation(summary = "타임캡슐 목록 조회", description = "로그인 사용자가 접근할 수 있는 캡슐을 최신순으로 조회합니다. 차단한 회원의 캡슐은 제외됩니다.")
	@GetMapping
	fun list(
		@LoginUser memberId: Long,
		@Parameter(description = "페이지 번호(0-base)", example = "0") @RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "페이지 크기(1~50)", example = "20") @RequestParam(defaultValue = "20") size: Int,
	) =
		ApiResponse.success(getCapsuleListService.execute(memberId, page, size))

	@Operation(
		summary = "주변 타임캡슐 조회",
		description = "좌표 기준 반경 안의 캡슐을 PostGIS 거리 계산으로 가까운 순부터 조회합니다. 캡슐의 정확한 위치는 열람 전까지 노출되지 않습니다.",
	)
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
		SwaggerApiResponse(responseCode = "400", description = "좌표 범위를 벗어나거나 반경이 허용 최대치를 초과"),
	)
	@GetMapping("/nearby")
	fun nearby(
		@LoginUser memberId: Long,
		@Parameter(description = "기준 위도", example = "37.5") @RequestParam latitude: Double,
		@Parameter(description = "기준 경도", example = "127.0") @RequestParam longitude: Double,
		@Parameter(description = "검색 반경(미터)", example = "1000") @RequestParam radius: Double,
		@Parameter(description = "페이지 번호(0-base)", example = "0") @RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "페이지 크기(1~50)", example = "20") @RequestParam(defaultValue = "20") size: Int,
	) = ApiResponse.success(getNearbyCapsuleService.execute(memberId, latitude, longitude, radius, page, size))

	@Operation(summary = "내가 만든 캡슐 목록 조회", description = "로그인 사용자가 묻은 캡슐을 최신순으로 조회합니다.")
	@GetMapping("/me")
	fun mine(
		@LoginUser memberId: Long,
		@Parameter(description = "페이지 번호(0-base)", example = "0") @RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "페이지 크기(1~50)", example = "20") @RequestParam(defaultValue = "20") size: Int,
	) =
		ApiResponse.success(getMyCapsuleListService.execute(memberId, page, size))

	@Operation(summary = "내가 받은 캡슐 목록 조회", description = "로그인 사용자가 수신자로 지정된 캡슐을 최신순으로 조회합니다.")
	@GetMapping("/received")
	fun received(
		@LoginUser memberId: Long,
		@Parameter(description = "페이지 번호(0-base)", example = "0") @RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "페이지 크기(1~50)", example = "20") @RequestParam(defaultValue = "20") size: Int,
	) =
		ApiResponse.success(getReceivedCapsuleListService.execute(memberId, page, size))

	@Operation(
		summary = "타임캡슐 상세 조회",
		description = "캡슐의 메타 정보를 조회합니다. 내용은 포함되지 않으며, 열람하려면 `POST /{capsuleId}/open` 을 호출해야 합니다.",
	)
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
		SwaggerApiResponse(responseCode = "403", description = "공개 범위·차단으로 접근 불가(CAPSULE_ACCESS_DENIED)"),
		SwaggerApiResponse(responseCode = "404", description = "캡슐 없음(CAPSULE_NOT_FOUND)"),
		SwaggerApiResponse(responseCode = "409", description = "만료된 캡슐(CAPSULE_EXPIRED)"),
	)
	@GetMapping("/{capsuleId}")
	fun detail(@LoginUser memberId: Long, @Parameter(description = "캡슐 ID") @PathVariable capsuleId: Long) =
		ApiResponse.success(getCapsuleDetailService.execute(memberId, capsuleId))

	@Operation(
		summary = "캡슐 열람",
		description = "현재 위치를 서버에서 PostGIS로 재검증한 뒤 캡슐 내용을 반환합니다. 최초 열람 시에만 잠금(비밀번호·질문)을 검증하며, " +
			"재열람은 위치만 다시 검증합니다. 최초 열람은 작성자에게 알림을 보내고, 공개 캡슐이면 발견 피드에 실립니다.",
	)
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "200", description = "열람 성공"),
		SwaggerApiResponse(responseCode = "403", description = "열람 반경 밖(CAPSULE_OUT_OF_RANGE), 잠금 검증 실패(CAPSULE_LOCK_FAILED), 접근 불가(CAPSULE_ACCESS_DENIED)"),
		SwaggerApiResponse(responseCode = "404", description = "캡슐 없음(CAPSULE_NOT_FOUND)"),
		SwaggerApiResponse(responseCode = "409", description = "아직 공개 시각 전(CAPSULE_NOT_OPEN_YET) 또는 만료됨(CAPSULE_EXPIRED)"),
	)
	@PostMapping("/{capsuleId}/open")
	fun open(@LoginUser memberId: Long, @Parameter(description = "캡슐 ID") @PathVariable capsuleId: Long, @Valid @RequestBody request: OpenCapsuleRequest) =
		ApiResponse.success(openCapsuleService.execute(memberId, capsuleId, request))

	@Operation(summary = "타임캡슐 삭제", description = "작성자만 삭제할 수 있으며 소프트 삭제됩니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "204", description = "삭제 성공"),
		SwaggerApiResponse(responseCode = "403", description = "작성자가 아님(CAPSULE_ACCESS_DENIED)"),
		SwaggerApiResponse(responseCode = "404", description = "캡슐 없음(CAPSULE_NOT_FOUND)"),
	)
	@DeleteMapping("/{capsuleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@LoginUser memberId: Long, @Parameter(description = "캡슐 ID") @PathVariable capsuleId: Long) = deleteCapsuleService.execute(memberId, capsuleId)

	@Operation(summary = "방명록 작성", description = "캡슐을 열람한 사용자만 방명록을 남길 수 있습니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "201", description = "작성 성공"),
		SwaggerApiResponse(responseCode = "403", description = "아직 열람하지 않은 캡슐(CAPSULE_ACCESS_DENIED)"),
		SwaggerApiResponse(responseCode = "404", description = "캡슐 없음(CAPSULE_NOT_FOUND)"),
	)
	@PostMapping("/{capsuleId}/guestbooks")
	fun createGuestbook(@LoginUser memberId: Long, @Parameter(description = "캡슐 ID") @PathVariable capsuleId: Long, @Valid @RequestBody request: CreateGuestbookRequest): ResponseEntity<ApiResponse<GuestbookResponse>> =
		ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createGuestbookService.execute(memberId, capsuleId, request)))

	@Operation(summary = "방명록 목록 조회", description = "캡슐에 남겨진 방명록을 최신순으로 조회합니다. 삭제된 방명록은 제외됩니다.")
	@GetMapping("/{capsuleId}/guestbooks")
	fun guestbooks(
		@LoginUser memberId: Long,
		@Parameter(description = "캡슐 ID") @PathVariable capsuleId: Long,
		@Parameter(description = "페이지 번호(0-base)", example = "0") @RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "페이지 크기(1~50)", example = "20") @RequestParam(defaultValue = "20") size: Int,
	) =
		ApiResponse.success(getGuestbookListService.execute(memberId, capsuleId, page, size))

	@Operation(summary = "방명록 수정", description = "작성자 본인만 수정할 수 있습니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "200", description = "수정 성공"),
		SwaggerApiResponse(responseCode = "403", description = "작성자가 아님(GUESTBOOK_ACCESS_DENIED)"),
		SwaggerApiResponse(responseCode = "404", description = "방명록 없음(GUESTBOOK_NOT_FOUND)"),
	)
	@PatchMapping("/{capsuleId}/guestbooks/{guestbookId}")
	fun updateGuestbook(
		@LoginUser memberId: Long,
		@Parameter(description = "캡슐 ID") @PathVariable capsuleId: Long,
		@Parameter(description = "방명록 ID") @PathVariable guestbookId: Long,
		@Valid @RequestBody request: UpdateGuestbookRequest,
	) =
		ApiResponse.success(updateGuestbookService.execute(memberId, capsuleId, guestbookId, request))

	@Operation(summary = "방명록 삭제", description = "작성자 본인만 삭제할 수 있으며 소프트 삭제됩니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "204", description = "삭제 성공"),
		SwaggerApiResponse(responseCode = "403", description = "작성자가 아님(GUESTBOOK_ACCESS_DENIED)"),
		SwaggerApiResponse(responseCode = "404", description = "방명록 없음(GUESTBOOK_NOT_FOUND)"),
	)
	@DeleteMapping("/{capsuleId}/guestbooks/{guestbookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun deleteGuestbook(
		@LoginUser memberId: Long,
		@Parameter(description = "캡슐 ID") @PathVariable capsuleId: Long,
		@Parameter(description = "방명록 ID") @PathVariable guestbookId: Long,
	) =
		deleteGuestbookService.execute(memberId, capsuleId, guestbookId)
}
