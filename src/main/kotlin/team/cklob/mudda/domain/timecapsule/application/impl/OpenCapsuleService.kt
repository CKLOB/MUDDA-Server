package team.cklob.mudda.domain.timecapsule.application.impl

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.timecapsule.application.CapsuleAccessPolicy
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleOpen
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleOpenRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleRecipientRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.presentation.request.OpenCapsuleRequest
import team.cklob.mudda.domain.timecapsule.presentation.response.MediaResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.OpenCapsuleResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.CapsuleException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime

@Service
class OpenCapsuleService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val openRepository: CapsuleOpenRepository,
	private val recipientRepository: CapsuleRecipientRepository,
	private val memberRepository: MemberRepository,
	private val mediaRepository: MediaRepository,
	private val mediaStorage: MediaStorage,
	private val passwordEncoder: PasswordEncoder,
	private val accessPolicy: CapsuleAccessPolicy,
) {
	@Transactional
	fun execute(memberId: Long, capsuleId: Long, request: OpenCapsuleRequest): OpenCapsuleResponse {
		val capsule = capsuleRepository.findByIdAndIsDeletedFalse(capsuleId).orElseThrow { CapsuleException() }
		val now = LocalDateTime.now()
		accessPolicy.requireAccessible(capsule, memberId, now)
		if (capsule.openAt.isAfter(now)) throw CapsuleException(ErrorCode.CAPSULE_NOT_OPEN_YET)
		if (!capsuleRepository.isWithinOpeningRadius(capsuleId, request.latitude, request.longitude)) {
			throw CapsuleException(ErrorCode.CAPSULE_OUT_OF_RANGE)
		}
		var opened = openRepository.findByTimeCapsuleIdAndMemberId(capsuleId, memberId).orElse(null)
		if (opened == null) {
			verifyLock(capsule.lockType, capsule.passwordHash, capsule.answerHash, request)
			val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
			val openLocation = GeometryFactory(PrecisionModel(), 4326).createPoint(Coordinate(request.longitude, request.latitude))
			opened = openRepository.save(CapsuleOpen(capsule, member, now, openLocation))
			recipientRepository.findByTimeCapsuleIdAndMemberId(capsuleId, memberId).ifPresent {
				it.hasOpened = true
				it.openedAt = now
			}
		}
		val media = mediaRepository.findAllByTimeCapsuleId(capsuleId).map {
			MediaResponse(requireNotNull(it.id), mediaStorage.createAccessUrl(it.s3Key).url, it.mediaType)
		}
		return OpenCapsuleResponse(capsuleId, capsule.name, capsule.content.orEmpty(), writer(capsule), media, opened.openedAt)
	}

	private fun verifyLock(lockType: CapsuleLockType, passwordHash: String?, answerHash: String?, request: OpenCapsuleRequest) {
		val matches = when (lockType) {
			CapsuleLockType.NONE -> true
			CapsuleLockType.PASSWORD -> request.password?.let { passwordEncoder.matches(it, passwordHash) } == true
			CapsuleLockType.QUESTION -> request.answer?.trim()?.lowercase()?.let { passwordEncoder.matches(it, answerHash) } == true
		}
		if (!matches) throw CapsuleException(ErrorCode.CAPSULE_LOCK_FAILED)
	}
}
