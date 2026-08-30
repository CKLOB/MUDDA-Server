package team.cklob.mudda.domain.member.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.cklob.mudda.domain.member.application.impl.GetMemberProfileService
import team.cklob.mudda.domain.member.application.impl.GetMyMemberService
import team.cklob.mudda.domain.member.application.impl.UpdateMyMemberService
import team.cklob.mudda.domain.member.presentation.request.UpdateMyMemberRequest
import team.cklob.mudda.domain.member.presentation.response.MemberProfileResponse
import team.cklob.mudda.domain.member.presentation.response.MyMemberResponse
import team.cklob.mudda.global.response.ApiResponse
import team.cklob.mudda.global.security.LoginUser

@Tag(name = "Member", description = "내 정보 조회·수정 및 다른 회원 프로필 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/members")
class MemberController(
	private val getMyMemberService: GetMyMemberService,
	private val updateMyMemberService: UpdateMyMemberService,
	private val getMemberProfileService: GetMemberProfileService,
) {
	@Operation(summary = "내 정보 조회", description = "로그인 사용자 본인의 정보를 조회합니다. 프로필 공개 범위와 무관하게 모든 필드가 반환됩니다.")
	@GetMapping("/me")
	fun getMe(@LoginUser memberId: Long): ResponseEntity<ApiResponse<MyMemberResponse>> =
		ResponseEntity.ok(ApiResponse.success(getMyMemberService.execute(memberId)))

	@Operation(summary = "내 정보 수정", description = "전달된 필드만 부분 수정합니다. 모든 필드를 생략하면 400을 반환합니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "200", description = "수정 성공"),
		SwaggerApiResponse(responseCode = "400", description = "수정할 필드가 하나도 없음"),
		SwaggerApiResponse(responseCode = "409", description = "닉네임 중복(NICKNAME_ALREADY_EXISTS)"),
	)
	@PatchMapping("/me")
	fun updateMe(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: UpdateMyMemberRequest,
	): ResponseEntity<ApiResponse<MyMemberResponse>> = ResponseEntity.ok(ApiResponse.success(updateMyMemberService.execute(memberId, request)))

	@Operation(
		summary = "프로필 조회",
		description = "다른 회원의 프로필과 나와의 친구 관계 상태를 조회합니다. 상대의 프로필 공개 범위에 따라 접근이 거부될 수 있습니다.",
	)
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
		SwaggerApiResponse(responseCode = "403", description = "프로필 공개 범위로 접근 불가(PROFILE_ACCESS_DENIED)"),
		SwaggerApiResponse(responseCode = "404", description = "회원 없음(MEMBER_NOT_FOUND)"),
	)
	@GetMapping("/{memberId}")
	fun getProfile(
		@LoginUser viewerId: Long,
		@Parameter(description = "조회할 회원 ID") @PathVariable memberId: Long,
	): ResponseEntity<ApiResponse<MemberProfileResponse>> = ResponseEntity.ok(ApiResponse.success(getMemberProfileService.execute(viewerId, memberId)))
}
