package team.cklob.mudda.domain.feed.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import team.cklob.mudda.domain.feed.application.impl.GetFeedListService
import team.cklob.mudda.domain.feed.infrastructure.FeedBroadcaster
import team.cklob.mudda.domain.feed.presentation.response.FeedListResponse
import team.cklob.mudda.global.response.ApiResponse

@Tag(name = "Feed", description = "발견 피드 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/feed")
class FeedController(
	private val getFeedListService: GetFeedListService,
	private val feedBroadcaster: FeedBroadcaster,
) {
	@Operation(summary = "발견 피드 목록 조회", description = "공개 캡슐이 열린 기록을 최신순으로 조회합니다.")
	@GetMapping
	fun getFeeds(@PageableDefault(size = 20) pageable: Pageable): ResponseEntity<ApiResponse<FeedListResponse>> =
		ResponseEntity.ok(ApiResponse.success(getFeedListService.execute(pageable)))

	@Operation(
		summary = "실시간 발견 피드 조회 (SSE)",
		description = "공개 캡슐이 열리는 즉시 `feed` 이벤트로 전달합니다. 연결 직후에는 헤더 플러시용 주석 이벤트가 한 번 전송됩니다.",
	)
	@GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
	fun stream(): SseEmitter = feedBroadcaster.subscribe(STREAM_TIMEOUT_MILLIS)

	private companion object {
		// Long enough that a client isn't reconnecting constantly, short enough that a half-open connection
		// behind a proxy is reaped instead of pinning a request thread forever.
		const val STREAM_TIMEOUT_MILLIS = 30 * 60 * 1000L
	}
}
