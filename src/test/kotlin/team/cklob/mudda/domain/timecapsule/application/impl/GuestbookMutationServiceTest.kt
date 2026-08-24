package team.cklob.mudda.domain.timecapsule.application.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import team.cklob.mudda.domain.timecapsule.application.CapsuleAccessPolicy
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.repository.GuestbookRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.presentation.request.UpdateGuestbookRequest
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.util.Optional
import kotlin.test.assertEquals

class GuestbookMutationServiceTest {
	private val capsuleRepository = mockk<TimeCapsuleRepository>()
	private val guestbookRepository = mockk<GuestbookRepository>()
	private val accessPolicy = mockk<CapsuleAccessPolicy>()

	@Test
	fun `update is blocked when the parent capsule is expired`() {
		val capsule = mockk<TimeCapsule>()
		every { capsuleRepository.findByIdAndIsDeletedFalse(1) } returns Optional.of(capsule)
		every { accessPolicy.requireAccessible(capsule, 7, any()) } throws BusinessException(ErrorCode.CAPSULE_EXPIRED)
		val service = UpdateGuestbookService(capsuleRepository, guestbookRepository, accessPolicy)

		val exception = assertThrows<BusinessException> {
			service.execute(7, 1, 2, UpdateGuestbookRequest("updated"))
		}

		assertEquals(ErrorCode.CAPSULE_EXPIRED, exception.errorCode)
		verify(exactly = 0) { guestbookRepository.findByIdAndTimeCapsuleIdAndIsDeletedFalse(any(), any()) }
	}

	@Test
	fun `delete is blocked when the parent capsule is deleted`() {
		every { capsuleRepository.findByIdAndIsDeletedFalse(1) } returns Optional.empty()
		val service = DeleteGuestbookService(capsuleRepository, guestbookRepository, accessPolicy)

		val exception = assertThrows<BusinessException> { service.execute(7, 1, 2) }

		assertEquals(ErrorCode.CAPSULE_NOT_FOUND, exception.errorCode)
		verify(exactly = 0) { guestbookRepository.findByIdAndTimeCapsuleIdAndIsDeletedFalse(any(), any()) }
	}
}
