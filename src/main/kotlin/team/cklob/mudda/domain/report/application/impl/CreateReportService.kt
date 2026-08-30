package team.cklob.mudda.domain.report.application.impl

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.report.domain.entity.Report
import team.cklob.mudda.domain.report.domain.repository.ReportRepository
import team.cklob.mudda.domain.report.domain.type.ReportReason
import team.cklob.mudda.domain.report.domain.type.ReportTargetType
import team.cklob.mudda.domain.report.presentation.request.CreateReportRequest
import team.cklob.mudda.domain.report.presentation.response.CreateReportResponse
import team.cklob.mudda.domain.timecapsule.domain.repository.GuestbookRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class CreateReportService(
	private val reportRepository: ReportRepository,
	private val memberRepository: MemberRepository,
	private val capsuleRepository: TimeCapsuleRepository,
	private val guestbookRepository: GuestbookRepository,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun execute(memberId: Long, request: CreateReportRequest): CreateReportResponse {
		val targetType = requireNotNull(request.targetType)
		val targetId = requireNotNull(request.targetId)
		val reason = requireNotNull(request.reason)

		// ETC carries no meaning on its own -- without a description the report is unactionable for whoever
		// reviews it, so it is rejected at the boundary rather than stored as noise.
		if (reason == ReportReason.ETC && request.description.isNullOrBlank()) {
			throw BusinessException(ErrorCode.INVALID_INPUT)
		}
		if (targetType == ReportTargetType.MEMBER && targetId == memberId) {
			throw BusinessException(ErrorCode.CANNOT_REPORT_SELF)
		}
		requireTargetExists(targetType, targetId, memberId)

		// uq_report_reporter_target enforces this too; checking first turns a constraint violation into a
		// meaningful 409 instead of a 500.
		if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(memberId, targetType, targetId)) {
			throw BusinessException(ErrorCode.ALREADY_REPORTED)
		}

		val reporter = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		val saved = try {
			reportRepository.saveAndFlush(
				Report(
					reporter = reporter,
					targetType = targetType,
					targetId = targetId,
					reason = reason,
					description = request.description?.trim(),
				),
			)
		} catch (e: DataIntegrityViolationException) {
			// Two concurrent reports of the same target both pass the check above and collide on
			// uq_report_reporter_target. The constraint is the real guarantee; this turns the loser into the
			// same 409 the sequential path returns instead of a 500. Nothing needs to be persisted here, so
			// letting the transaction roll back is the correct outcome.
			logger.debug("concurrent duplicate report: reporter={}, target={}:{}", memberId, targetType, targetId, e)
			throw BusinessException(ErrorCode.ALREADY_REPORTED)
		}
		return CreateReportResponse.from(saved)
	}

	// A report against something that does not exist is worthless to a reviewer, and accepting one lets a
	// caller probe which ids exist. Both are avoided by verifying the target up front.
	private fun requireTargetExists(targetType: ReportTargetType, targetId: Long, memberId: Long) {
		val exists = when (targetType) {
			ReportTargetType.MEMBER -> memberRepository.findById(targetId).filter { it.withdrawnAt == null }.isPresent
			ReportTargetType.CAPSULE -> capsuleRepository.findById(targetId).filter { !it.isDeleted }.isPresent
			ReportTargetType.GUESTBOOK -> guestbookRepository.findById(targetId).filter { !it.isDeleted }.isPresent
		}
		if (!exists) throw BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND)
	}
}
