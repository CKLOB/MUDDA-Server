package team.cklob.mudda.domain.report.domain.repository

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility
import team.cklob.mudda.domain.report.domain.entity.Report
import team.cklob.mudda.domain.report.domain.type.ReportReason
import team.cklob.mudda.domain.report.domain.type.ReportTargetType
import team.cklob.mudda.support.PostgresIntegrationTest
import kotlin.test.assertEquals

// CreateReportService relies on uq_report_reporter_target to be the real duplicate guarantee and maps the
// resulting violation to 409, so the constraint's actual behaviour is pinned here.
class ReportConcurrencyIntegrationTest(
	@Autowired private val reportRepository: ReportRepository,
	@Autowired private val memberRepository: MemberRepository,
) : PostgresIntegrationTest() {
	private fun member(tag: String) = memberRepository.saveAndFlush(
		Member(
			name = "name", nickname = "nick-$tag", email = "report-$tag@example.com",
			oauthProvider = OAuthProvider.GOOGLE, providerId = "report-provider-$tag",
			profileVisibility = ProfileVisibility.PUBLIC,
		),
	)

	private fun report(reporter: Member, targetId: Long, targetType: ReportTargetType = ReportTargetType.CAPSULE) =
		reportRepository.saveAndFlush(Report(reporter, targetType, targetId, ReportReason.ABUSE))

	@Test fun `reporting the same target twice violates the unique constraint`() {
		val reporter = member("a")
		report(reporter, 100)

		assertThrows<DataIntegrityViolationException> { report(reporter, 100) }
	}

	@Test fun `the constraint is scoped by target type so the same id in another type is allowed`() {
		val reporter = member("b")
		report(reporter, 200, ReportTargetType.CAPSULE)

		report(reporter, 200, ReportTargetType.GUESTBOOK)

		assertEquals(2, reportRepository.count())
	}

	@Test fun `enum values round-trip through the varchar columns`() {
		val reporter = member("c")
		val saved = report(reporter, 300)

		val found = reportRepository.findById(requireNotNull(saved.id)).orElseThrow()

		assertEquals(ReportTargetType.CAPSULE, found.targetType)
		assertEquals(ReportReason.ABUSE, found.reason)
	}
}
