package team.cklob.mudda.domain.media.presentation.controller

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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.cklob.mudda.domain.media.application.impl.CompleteMediaUploadService
import team.cklob.mudda.domain.media.application.impl.CreateMediaUploadUrlService
import team.cklob.mudda.domain.media.application.impl.DeleteMediaService
import team.cklob.mudda.domain.media.presentation.request.CompleteMediaUploadRequest
import team.cklob.mudda.domain.media.presentation.request.CreateMediaUploadUrlRequest
import team.cklob.mudda.domain.media.presentation.response.CompleteMediaUploadResponse
import team.cklob.mudda.domain.media.presentation.response.CreateMediaUploadUrlResponse
import team.cklob.mudda.global.response.ApiResponse
import team.cklob.mudda.global.security.LoginUser

@Tag(name = "Media", description = "S3 Presigned URL 기반 미디어 업로드 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/media")
class MediaController(
	private val createMediaUploadUrlService: CreateMediaUploadUrlService,
	private val completeMediaUploadService: CompleteMediaUploadService,
	private val deleteMediaService: DeleteMediaService,
) {
	@Operation(
		summary = "미디어 업로드 URL 발급",
		description = "S3에 직접 업로드할 Presigned URL과 업로드 키를 발급합니다. 파일을 해당 URL에 PUT 한 뒤 " +
			"`POST /api/v1/media` 로 업로드 완료를 등록해야 실제 미디어로 등록됩니다.",
	)
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "200", description = "발급 성공"),
		SwaggerApiResponse(responseCode = "400", description = "허용되지 않는 확장자·용량(INVALID_MEDIA_UPLOAD)"),
	)
	@PostMapping("/upload-urls")
	fun createUploadUrl(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: CreateMediaUploadUrlRequest,
	): ResponseEntity<ApiResponse<CreateMediaUploadUrlResponse>> =
		ResponseEntity.ok(ApiResponse.success(createMediaUploadUrlService.execute(memberId, request)))

	@Operation(
		summary = "미디어 업로드 완료 등록",
		description = "Presigned URL 업로드를 마친 뒤 호출합니다. 서버가 S3 객체의 실제 Content-Type과 크기를 재검증한 후 " +
			"미디어로 등록합니다. 같은 업로드 키로 다시 호출해도 안전합니다.",
	)
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "201", description = "등록 성공"),
		SwaggerApiResponse(responseCode = "400", description = "업로드 키가 잘못되었거나 검증 실패(INVALID_MEDIA_UPLOAD)"),
		SwaggerApiResponse(responseCode = "502", description = "스토리지 요청 실패(MEDIA_STORAGE_ERROR)"),
	)
	@PostMapping
	fun completeUpload(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: CompleteMediaUploadRequest,
	): ResponseEntity<ApiResponse<CompleteMediaUploadResponse>> =
		ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(completeMediaUploadService.execute(memberId, request)))

	@Operation(summary = "미디어 삭제", description = "아직 캡슐에 첨부되지 않은 본인 소유 미디어만 삭제할 수 있습니다.")
	@SwaggerApiResponses(
		SwaggerApiResponse(responseCode = "204", description = "삭제 성공"),
		SwaggerApiResponse(responseCode = "404", description = "미디어 없음 또는 본인 소유가 아님(MEDIA_NOT_FOUND)"),
		SwaggerApiResponse(responseCode = "409", description = "이미 캡슐에 첨부된 미디어(MEDIA_ALREADY_ATTACHED)"),
	)
	@DeleteMapping("/{mediaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@LoginUser memberId: Long, @Parameter(description = "삭제할 미디어 ID") @PathVariable mediaId: Long) {
		deleteMediaService.execute(memberId, mediaId)
	}
}
