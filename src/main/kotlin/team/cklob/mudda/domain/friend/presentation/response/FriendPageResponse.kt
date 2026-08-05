package team.cklob.mudda.domain.friend.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

// Minimal page wrapper shared by the Friend domain's three list endpoints (friend list, search,
// request list). Spring's own PageImpl serializes a lot of pageable/sort internals that clients don't
// need, and the project has no existing common page response to reuse -- this is a right-sized
// substitute rather than a project-wide abstraction.
@Schema(description = "페이지 응답")
data class FriendPageResponse<T>(
	@Schema(description = "현재 페이지의 데이터 목록")
	val content: List<T>,

	@Schema(description = "현재 페이지 번호(0-base)", example = "0")
	val page: Int,

	@Schema(description = "페이지 크기", example = "20")
	val size: Int,

	@Schema(description = "전체 요소 수", example = "3")
	val totalElements: Long,

	@Schema(description = "전체 페이지 수", example = "1")
	val totalPages: Int,

	@Schema(description = "다음 페이지 존재 여부", example = "false")
	val hasNext: Boolean,
) {
	companion object {
		fun <S, T> of(page: Page<S>, content: List<T>) = FriendPageResponse(
			content = content,
			page = page.number,
			size = page.size,
			totalElements = page.totalElements,
			totalPages = page.totalPages,
			hasNext = page.hasNext(),
		)
	}
}
