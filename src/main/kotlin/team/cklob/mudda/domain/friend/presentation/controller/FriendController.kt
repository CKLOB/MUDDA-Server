package team.cklob.mudda.domain.friend.presentation.controller

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
import team.cklob.mudda.domain.friend.application.impl.DeleteFriendService
import team.cklob.mudda.domain.friend.application.impl.GetFriendListService
import team.cklob.mudda.domain.friend.application.impl.GetFriendRequestListService
import team.cklob.mudda.domain.friend.application.impl.RespondFriendRequestService
import team.cklob.mudda.domain.friend.application.impl.SearchFriendService
import team.cklob.mudda.domain.friend.application.impl.SendFriendRequestService
import team.cklob.mudda.domain.friend.domain.type.FriendRequestStatus
import team.cklob.mudda.domain.friend.domain.type.FriendRequestType
import team.cklob.mudda.domain.friend.presentation.request.RespondFriendRequestRequest
import team.cklob.mudda.domain.friend.presentation.request.SendFriendRequestRequest
import team.cklob.mudda.domain.friend.presentation.response.FriendPageResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendRequestResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendSearchResponse
import team.cklob.mudda.domain.friend.presentation.response.SendFriendRequestResponse
import team.cklob.mudda.global.response.ApiResponse
import team.cklob.mudda.global.security.LoginUser

@Tag(name = "Friend", description = "친구 목록/검색/요청/삭제 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/friends")
class FriendController(
	private val getFriendListService: GetFriendListService,
	private val searchFriendService: SearchFriendService,
	private val sendFriendRequestService: SendFriendRequestService,
	private val getFriendRequestListService: GetFriendRequestListService,
	private val respondFriendRequestService: RespondFriendRequestService,
	private val deleteFriendService: DeleteFriendService,
) {
	@Operation(summary = "친구 목록 조회", description = "로그인 사용자의 ACCEPTED 상태 친구 목록을 최근 친구가 된 순으로 조회합니다.")
	@GetMapping
	fun getFriends(
		@LoginUser memberId: Long,
		@PageableDefault(size = 20) pageable: Pageable,
	): ResponseEntity<ApiResponse<FriendPageResponse<FriendResponse>>> =
		ResponseEntity.ok(ApiResponse.success(getFriendListService.execute(memberId, pageable)))

	@Operation(summary = "사용자 검색", description = "닉네임으로 다른 회원을 검색하고, 로그인 사용자와의 친구 관계 상태를 함께 반환합니다.")
	@GetMapping("/search")
	fun search(
		@LoginUser memberId: Long,
		@Parameter(description = "검색 닉네임 키워드", example = "nick") @RequestParam keyword: String,
		@PageableDefault(size = 20) pageable: Pageable,
	): ResponseEntity<ApiResponse<FriendPageResponse<FriendSearchResponse>>> =
		ResponseEntity.ok(ApiResponse.success(searchFriendService.execute(memberId, keyword, pageable)))

	@Operation(summary = "친구 요청 전송", description = "다른 회원에게 친구 요청을 보냅니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "201", description = "요청 생성 성공"),
		SwaggerApiResponse(responseCode = "400", description = "자기 자신에게 요청(CANNOT_REQUEST_SELF) 등 잘못된 입력"),
		SwaggerApiResponse(responseCode = "403", description = "차단 관계로 요청 불가(BLOCKED_MEMBER)"),
		SwaggerApiResponse(responseCode = "404", description = "대상 회원 없음(MEMBER_NOT_FOUND)"),
		SwaggerApiResponse(responseCode = "409", description = "이미 친구이거나(ALREADY_FRIENDS) 중복/역방향 대기 요청 존재"),
	)
	@PostMapping("/requests")
	@ResponseStatus(HttpStatus.CREATED)
	fun sendRequest(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: SendFriendRequestRequest,
	): ApiResponse<SendFriendRequestResponse> = ApiResponse.success(sendFriendRequestService.execute(memberId, request))

	@Operation(summary = "친구 요청 목록 조회", description = "받은(RECEIVED) 또는 보낸(SENT) 친구 요청 목록을 조회합니다. 기본적으로 PENDING 상태만 반환합니다.")
	@GetMapping("/requests")
	fun getRequests(
		@LoginUser memberId: Long,
		@Parameter(description = "요청 방향", example = "RECEIVED") @RequestParam type: FriendRequestType,
		@Parameter(description = "요청 상태 필터", example = "PENDING") @RequestParam(defaultValue = "PENDING") status: FriendRequestStatus,
		@PageableDefault(size = 20) pageable: Pageable,
	): ResponseEntity<ApiResponse<FriendPageResponse<FriendRequestResponse>>> =
		ResponseEntity.ok(ApiResponse.success(getFriendRequestListService.execute(memberId, type, status, pageable)))

	@Operation(summary = "친구 요청 수락/거절", description = "요청 수신자만 자신이 받은 PENDING 요청을 ACCEPT 또는 REJECT 할 수 있습니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "204", description = "처리 성공"),
		SwaggerApiResponse(responseCode = "400", description = "action 누락 또는 지원하지 않는 값"),
		SwaggerApiResponse(responseCode = "403", description = "요청 수신자가 아님(FRIEND_REQUEST_NOT_RECEIVER)"),
		SwaggerApiResponse(responseCode = "404", description = "요청 없음(FRIEND_REQUEST_NOT_FOUND)"),
		SwaggerApiResponse(responseCode = "409", description = "이미 처리된 요청(FRIEND_REQUEST_ALREADY_PROCESSED)"),
	)
	@PatchMapping("/requests/{requestId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun respondToRequest(
		@LoginUser memberId: Long,
		@PathVariable requestId: Long,
		@Valid @RequestBody request: RespondFriendRequestRequest,
	) {
		respondFriendRequestService.execute(memberId, requestId, request)
	}

	@Operation(summary = "친구 삭제", description = "ACCEPTED 상태인 친구 관계를 삭제합니다. 두 당사자 중 누구든 호출할 수 있습니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "204", description = "삭제 성공"),
		SwaggerApiResponse(responseCode = "404", description = "ACCEPTED 상태의 친구 관계 없음(FRIEND_NOT_FOUND)"),
	)
	@DeleteMapping("/{memberId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun deleteFriend(
		@LoginUser loginMemberId: Long,
		@Parameter(description = "삭제할 친구의 회원 ID") @PathVariable("memberId") targetMemberId: Long,
	) {
		deleteFriendService.execute(loginMemberId, targetMemberId)
	}
}
