package team.cklob.mudda.domain.report.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.report.domain.entity.Report
import team.cklob.mudda.domain.report.domain.repository.ReportRepository
import team.cklob.mudda.domain.report.domain.type.ReportReason
import team.cklob.mudda.domain.report.domain.type.ReportTargetType
import team.cklob.mudda.domain.report.presentation.request.CreateReportRequest
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.repository.GuestbookRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

class CreateReportServiceTest {
	private val reportRepository = mockk<ReportRepository>()
	private val memberRepository = mockk<MemberRepository>()
	private val capsuleRepository = mockk<TimeCapsuleRepository>()
	private val guestbookRepository = mockk<GuestbookRepository>()
	private val service = CreateReportService(reportRepository, memberRepository, capsuleRepository, guestbookRepository)

	private fun member(id: Long) = Member(
		name = "name", nickname = "nick$id", email = "a$id@example.com", oauthProvider = OAuthProvider.GOOGLE,
		providerId = "provider$id", profileVisibility = ProfileVisibility.PUBLIC, id = id,
	)

	private fun capsule(isDeleted: Boolean = false) = TimeCapsule(
		member = member(2), name = "capsule", visibility = CapsuleVisibility.PUBLIC, lockType = CapsuleLockType.NONE,
		location = GeometryFactory().createPoint(Coordinate(127.0, 37.5)), openRadiusMeter = 100,
		openAt = LocalDateTime.now(), isDeleted = isDeleted, id = 5,
	)

	@Test fun `reporting yourself is rejected`() {
		val request = CreateReportRequest(ReportTargetType.MEMBER, 1, ReportReason.ABUSE)

		val error = assertThrows<BusinessException> { service.execute(1, request) }

		assertEquals(ErrorCode.CANNOT_REPORT_SELF, error.errorCode)
	}

	// ETC carries no meaning on its own; without a description the report is unactionable for a reviewer.
	@Test fun `ETC without a description is rejected`() {
		val request = CreateReportRequest(ReportTargetType.CAPSULE, 5, ReportReason.ETC, description = "  ")

		val error = assertThrows<BusinessException> { service.execute(1, request) }

		assertEquals(ErrorCode.INVALID_INPUT, error.errorCode)
	}

	@Test fun `reporting a deleted capsule reports the target as missing`() {
		every { capsuleRepository.findById(5) } returns Optional.of(capsule(isDeleted = true))
		val request = CreateReportRequest(ReportTargetType.CAPSULE, 5, ReportReason.ABUSE)

		val error = assertThrows<BusinessException> { service.execute(1, request) }

		assertEquals(ErrorCode.REPORT_TARGET_NOT_FOUND, error.errorCode)
	}

	@Test fun `reporting the same target twice is a conflict`() {
		every { capsuleRepository.findById(5) } returns Optional.of(capsule())
		every { reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1, ReportTargetType.CAPSULE, 5) } returns true
		val request = CreateReportRequest(ReportTargetType.CAPSULE, 5, ReportReason.ABUSE)

		val error = assertThrows<BusinessException> { service.execute(1, request) }

		assertEquals(ErrorCode.ALREADY_REPORTED, error.errorCode)
		verify(exactly = 0) { reportRepository.saveAndFlush(any()) }
	}

	@Test fun `a valid capsule report is stored`() {
		every { capsuleRepository.findById(5) } returns Optional.of(capsule())
		every { reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1, ReportTargetType.CAPSULE, 5) } returns false
		every { memberRepository.findById(1) } returns Optional.of(member(1))
		every { reportRepository.saveAndFlush(any()) } answers {
			Report(member(1), ReportTargetType.CAPSULE, 5, ReportReason.ABUSE, "욕설", id = 7)
		}
		val request = CreateReportRequest(ReportTargetType.CAPSULE, 5, ReportReason.ABUSE, "욕설")

		val response = service.execute(1, request)

		assertEquals(7, response.reportId)
		assertEquals(ReportReason.ABUSE, response.reason)
	}

	// A concurrent duplicate slips past the exists check and is stopped by uq_report_reporter_target. The
	// violation must surface as the same 409 the sequential path returns, not a 500.
	@Test fun `a unique violation from a concurrent duplicate becomes ALREADY_REPORTED`() {
		every { capsuleRepository.findById(5) } returns Optional.of(capsule())
		every { reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1, ReportTargetType.CAPSULE, 5) } returns false
		every { memberRepository.findById(1) } returns Optional.of(member(1))
		every { reportRepository.saveAndFlush(any()) } throws DataIntegrityViolationException("uq_report_reporter_target")
		val request = CreateReportRequest(ReportTargetType.CAPSULE, 5, ReportReason.ABUSE)

		val error = assertThrows<BusinessException> { service.execute(1, request) }

		assertEquals(ErrorCode.ALREADY_REPORTED, error.errorCode)
	}
}
