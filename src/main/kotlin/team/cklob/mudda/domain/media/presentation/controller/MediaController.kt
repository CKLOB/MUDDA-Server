package team.cklob.mudda.domain.media.presentation.controller

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

@RestController
@RequestMapping("/api/v1/media")
class MediaController(
	private val createMediaUploadUrlService: CreateMediaUploadUrlService,
	private val completeMediaUploadService: CompleteMediaUploadService,
	private val deleteMediaService: DeleteMediaService,
) {
	@PostMapping("/upload-urls")
	fun createUploadUrl(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: CreateMediaUploadUrlRequest,
	): ResponseEntity<ApiResponse<CreateMediaUploadUrlResponse>> =
		ResponseEntity.ok(ApiResponse.success(createMediaUploadUrlService.execute(memberId, request)))

	@PostMapping
	fun completeUpload(
		@LoginUser memberId: Long,
		@Valid @RequestBody request: CompleteMediaUploadRequest,
	): ResponseEntity<ApiResponse<CompleteMediaUploadResponse>> =
		ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(completeMediaUploadService.execute(memberId, request)))

	@DeleteMapping("/{mediaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@LoginUser memberId: Long, @PathVariable mediaId: Long) {
		deleteMediaService.execute(memberId, mediaId)
	}
}
