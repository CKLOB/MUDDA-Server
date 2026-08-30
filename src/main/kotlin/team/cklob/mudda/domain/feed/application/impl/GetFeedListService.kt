package team.cklob.mudda.domain.feed.application.impl

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.feed.presentation.response.FeedListResponse
import team.cklob.mudda.domain.feed.presentation.response.FeedResponse
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleOpenRepository

@Service
class GetFeedListService(
	private val capsuleOpenRepository: CapsuleOpenRepository,
) {
	@Transactional(readOnly = true)
	fun execute(pageable: Pageable): FeedListResponse =
		FeedListResponse(capsuleOpenRepository.findPublicFeed(pageable).content.map(FeedResponse::from))
}
