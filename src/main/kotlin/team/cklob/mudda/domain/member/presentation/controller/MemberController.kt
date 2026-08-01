package team.cklob.mudda.domain.member.presentation.controller

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

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
	private val getMyMemberService: GetMyMemberService,
	private val updateMyMemberService: UpdateMyMemberService,
	private val getMemberProfileService: GetMemberProfileService,
) {
	@GetMapping("/me")
	fun getMe(@LoginUser memberId: Long): ResponseEntity<ApiResponse<MyMemberResponse>> =
		ResponseEntity.ok(ApiResponse.success(getMyMemberService.execute(memberId)))

	@PatchMapping("/me")
	fun updateMe(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: UpdateMyMemberRequest,
	): ResponseEntity<ApiResponse<MyMemberResponse>> = ResponseEntity.ok(ApiResponse.success(updateMyMemberService.execute(memberId, request)))

	@GetMapping("/{memberId}")
	fun getProfile(
		@LoginUser viewerId: Long,
		@PathVariable memberId: Long,
	): ResponseEntity<ApiResponse<MemberProfileResponse>> = ResponseEntity.ok(ApiResponse.success(getMemberProfileService.execute(viewerId, memberId)))
}
