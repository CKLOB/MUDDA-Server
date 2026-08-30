package team.cklob.mudda.domain.block.application.impl

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.block.presentation.request.CreateBlockRequest
import team.cklob.mudda.domain.block.presentation.response.BlockResponse
import team.cklob.mudda.domain.block.presentation.response.CreateBlockResponse
import team.cklob.mudda.domain.friend.presentation.response.FriendPageResponse
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

// Blocking is purely additive: it writes one tbl_block row and nothing else. Every read path already
// excludes blocked members in SQL (friend list, friend requests, member search, capsule access), so there
// is no friendship or pending-request cascade to keep in sync -- and unblocking restores the prior state
// for free.
@Service
class CreateBlockService(
	private val blockRepository: BlockRepository,
	private val memberRepository: MemberRepository,
) {
	@Transactional
	fun execute(memberId: Long, request: CreateBlockRequest): CreateBlockResponse {
		val targetId = requireNotNull(request.memberId)
		if (targetId == memberId) throw BusinessException(ErrorCode.CANNOT_BLOCK_SELF)

		val target = memberRepository.findById(targetId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		if (target.withdrawnAt != null) throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
		if (!memberRepository.existsById(memberId)) throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)

		// Blocking twice is the same end state as blocking once, so the existing row is returned rather than
		// raising a conflict the client would have to special-case. The insert is atomic so two concurrent
		// requests both get that answer instead of one of them hitting uq_block_blocker_blocked: the loser
		// simply sees 0 rows affected and reads back the winner's row.
		blockRepository.insertIfAbsent(memberId, targetId)
		val block = blockRepository.findByBlockerIdAndBlockedId(memberId, targetId)
			.orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		return CreateBlockResponse(requireNotNull(block.id), targetId, block.createdAt)
	}
}

@Service
class DeleteBlockService(
	private val blockRepository: BlockRepository,
) {
	@Transactional
	fun execute(memberId: Long, targetMemberId: Long) {
		val block = blockRepository.findByBlockerIdAndBlockedId(memberId, targetMemberId)
			.orElseThrow { BusinessException(ErrorCode.BLOCK_NOT_FOUND) }
		blockRepository.delete(block)
	}
}

@Service
class GetBlockListService(
	private val blockRepository: BlockRepository,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, pageable: Pageable): FriendPageResponse<BlockResponse> {
		val page = blockRepository.findByBlockerIdOrderByCreatedAtDesc(memberId, pageable)
		return FriendPageResponse.of(page, page.content.map(BlockResponse::from))
	}
}
