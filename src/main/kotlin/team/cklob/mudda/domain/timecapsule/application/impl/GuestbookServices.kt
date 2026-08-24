package team.cklob.mudda.domain.timecapsule.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.timecapsule.application.CapsuleAccessPolicy
import team.cklob.mudda.domain.timecapsule.domain.entity.Guestbook
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleOpenRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.GuestbookRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.presentation.request.CreateGuestbookRequest
import team.cklob.mudda.domain.timecapsule.presentation.request.UpdateGuestbookRequest
import team.cklob.mudda.domain.timecapsule.presentation.response.GuestbookPageResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.GuestbookResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.UpdateGuestbookResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.CapsuleException
import team.cklob.mudda.global.exception.ErrorCode

private fun requireOpened(capsuleId: Long, memberId: Long, openRepository: CapsuleOpenRepository) {
	if (!openRepository.existsByTimeCapsuleIdAndMemberId(capsuleId, memberId)) {
		throw CapsuleException(ErrorCode.GUESTBOOK_ACCESS_DENIED)
	}
}

@Service
class CreateGuestbookService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val guestbookRepository: GuestbookRepository,
	private val openRepository: CapsuleOpenRepository,
	private val memberRepository: MemberRepository,
	private val accessPolicy: CapsuleAccessPolicy,
) {
	@Transactional
	fun execute(memberId: Long, capsuleId: Long, request: CreateGuestbookRequest): GuestbookResponse {
		val capsule = capsuleRepository.findByIdAndIsDeletedFalse(capsuleId).orElseThrow { CapsuleException() }
		accessPolicy.requireAccessible(capsule, memberId)
		requireOpened(capsuleId, memberId, openRepository)
		val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		return guestbookRepository.save(Guestbook(capsule, member, request.content.trim())).toResponse()
	}
}

@Service
class GetGuestbookListService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val guestbookRepository: GuestbookRepository,
	private val openRepository: CapsuleOpenRepository,
	private val accessPolicy: CapsuleAccessPolicy,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, capsuleId: Long, pageNumber: Int, size: Int): GuestbookPageResponse {
		val capsule = capsuleRepository.findByIdAndIsDeletedFalse(capsuleId).orElseThrow { CapsuleException() }
		accessPolicy.requireAccessible(capsule, memberId)
		requireOpened(capsuleId, memberId, openRepository)
		val values = guestbookRepository.findByTimeCapsuleIdAndIsDeletedFalseOrderByCreatedAtDesc(capsuleId).map { it.toResponse() }
		val (items, current, total) = page(values, pageNumber, size)
		return GuestbookPageResponse(items, current, size, total)
	}
}

@Service
class UpdateGuestbookService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val guestbookRepository: GuestbookRepository,
	private val accessPolicy: CapsuleAccessPolicy,
) {
	@Transactional
	fun execute(memberId: Long, capsuleId: Long, guestbookId: Long, request: UpdateGuestbookRequest): UpdateGuestbookResponse {
		val capsule = capsuleRepository.findByIdAndIsDeletedFalse(capsuleId).orElseThrow { CapsuleException() }
		accessPolicy.requireAccessible(capsule, memberId)
		val guestbook = guestbookRepository.findByIdAndTimeCapsuleIdAndIsDeletedFalse(guestbookId, capsuleId)
			?: throw CapsuleException(ErrorCode.GUESTBOOK_NOT_FOUND)
		if (guestbook.member.id != memberId) throw CapsuleException(ErrorCode.GUESTBOOK_ACCESS_DENIED)
		guestbook.content = request.content.trim()
		guestbookRepository.flush()
		return UpdateGuestbookResponse(requireNotNull(guestbook.id), guestbook.content, guestbook.updatedAt)
	}
}

@Service
class DeleteGuestbookService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val guestbookRepository: GuestbookRepository,
	private val accessPolicy: CapsuleAccessPolicy,
) {
	@Transactional
	fun execute(memberId: Long, capsuleId: Long, guestbookId: Long) {
		val capsule = capsuleRepository.findByIdAndIsDeletedFalse(capsuleId).orElseThrow { CapsuleException() }
		accessPolicy.requireAccessible(capsule, memberId)
		val guestbook = guestbookRepository.findByIdAndTimeCapsuleIdAndIsDeletedFalse(guestbookId, capsuleId)
			?: throw CapsuleException(ErrorCode.GUESTBOOK_NOT_FOUND)
		if (guestbook.member.id != memberId) throw CapsuleException(ErrorCode.GUESTBOOK_ACCESS_DENIED)
		guestbook.isDeleted = true
	}
}
