package team.cklob.mudda.domain.block.presentation.controller

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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.cklob.mudda.domain.block.application.impl.CreateBlockService
import team.cklob.mudda.domain.block.application.impl.DeleteBlockService
import team.cklob.mudda.domain.block.application.impl.GetBlockListService
import team.cklob.mudda.domain.block.presentation.request.CreateBlockRequest
import team.cklob.mudda.domain.block.presentation.response.BlockResponse
import team.cklob.mudda.domain.block.presentation.response.CreateBlockResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendPageResponse
import team.cklob.mudda.global.response.ApiResponse
import team.cklob.mudda.global.security.LoginUser

@Tag(name = "Block", description = "회원 차단 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/blocks")
class BlockController(
	private val createBlockService: CreateBlockService,
	private val deleteBlockService: DeleteBlockService,
	private val getBlockListService: GetBlockListService,
) {
	@Operation(
		summary = "회원 차단",
		description = "대상 회원을 차단합니다. 차단 후에는 친구 목록·친구 요청·사용자 검색·캡슐 접근에서 서로가 보이지 않습니다. " +
			"친구 관계나 대기 중인 요청을 삭제하지는 않으므로, 차단을 해제하면 이전 상태가 그대로 복원됩니다.",
	)
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "201", description = "차단 성공. 이미 차단한 회원이면 기존 차단 정보를 그대로 반환합니다."),
		SwaggerApiResponse(responseCode = "400", description = "자기 자신을 차단(CANNOT_BLOCK_SELF)"),
		SwaggerApiResponse(responseCode = "404", description = "대상 회원 없음(MEMBER_NOT_FOUND)"),
	)
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun createBlock(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: CreateBlockRequest,
	): ApiResponse<CreateBlockResponse> = ApiResponse.success(createBlockService.execute(memberId, request))

	@Operation(summary = "차단 목록 조회", description = "로그인 사용자가 차단한 회원 목록을 최근 차단순으로 조회합니다.")
	@GetMapping
	fun getBlocks(
		@LoginUser memberId: Long,
		@PageableDefault(size = 20) pageable: Pageable,
	): ResponseEntity<ApiResponse<FriendPageResponse<BlockResponse>>> =
		ResponseEntity.ok(ApiResponse.success(getBlockListService.execute(memberId, pageable)))

	@Operation(summary = "차단 해제", description = "차단을 해제합니다. 차단 이전의 친구 관계와 대기 중인 요청이 다시 보이게 됩니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "204", description = "해제 성공"),
		SwaggerApiResponse(responseCode = "404", description = "차단 기록 없음(BLOCK_NOT_FOUND)"),
	)
	@DeleteMapping("/{memberId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun deleteBlock(
		@LoginUser loginMemberId: Long,
		@Parameter(description = "차단을 해제할 회원 ID") @PathVariable("memberId") targetMemberId: Long,
	) {
		deleteBlockService.execute(loginMemberId, targetMemberId)
	}
}
