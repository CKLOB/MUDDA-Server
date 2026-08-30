package team.cklob.mudda.domain.report.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses as SwaggerApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.cklob.mudda.domain.report.application.impl.CreateReportService
import team.cklob.mudda.domain.report.presentation.request.CreateReportRequest
import team.cklob.mudda.domain.report.presentation.response.CreateReportResponse
import team.cklob.mudda.global.response.ApiResponse
import team.cklob.mudda.global.security.LoginUser

@Tag(name = "Report", description = "회원·캡슐·방명록 신고 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/reports")
class ReportController(
	private val createReportService: CreateReportService,
) {
	@Operation(
		summary = "신고 접수",
		description = "회원, 캡슐, 방명록을 신고합니다. 같은 대상을 중복 신고할 수 없으며, 사유가 ETC이면 description이 필수입니다.",
	)
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "201", description = "접수 성공"),
		SwaggerApiResponse(responseCode = "400", description = "자기 자신 신고(CANNOT_REPORT_SELF) 또는 ETC 사유에 상세 설명 누락"),
		SwaggerApiResponse(responseCode = "404", description = "신고 대상 없음(REPORT_TARGET_NOT_FOUND)"),
		SwaggerApiResponse(responseCode = "409", description = "이미 신고한 대상(ALREADY_REPORTED)"),
	)
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun createReport(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: CreateReportRequest,
	): ApiResponse<CreateReportResponse> = ApiResponse.success(createReportService.execute(memberId, request))
}
