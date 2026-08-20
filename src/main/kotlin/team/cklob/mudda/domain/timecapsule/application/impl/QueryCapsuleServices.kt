package team.cklob.mudda.domain.timecapsule.application.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.timecapsule.application.CapsuleAccessPolicy
import team.cklob.mudda.domain.timecapsule.application.CapsuleProperties
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleOpenRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleRecipientRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.presentation.response.CapsuleDetailResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.CapsuleListItemResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.CapsulePageResponse
import team.cklob.mudda.domain.timecapsule.presentation.response.NearbyCapsuleResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.CapsuleException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime

@Service
class GetCapsuleListService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val openRepository: CapsuleOpenRepository,
	private val accessPolicy: CapsuleAccessPolicy,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, pageNumber: Int, size: Int): CapsulePageResponse<CapsuleListItemResponse> {
		val now = LocalDateTime.now()
		// ponytail: in-memory access filtering keeps the authorization rule in one place; move it into a
		// paged repository query when capsule volume makes this visible in query latency.
		val visible = capsuleRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()
			.filter { it.expiredAt?.isAfter(now) != false && accessPolicy.canAccess(it, memberId) }
			.map { it.toListItem(openRepository.existsByTimeCapsuleIdAndMemberId(requireNotNull(it.id), memberId)) }
		val (items, current, total) = page(visible, pageNumber, size)
		return CapsulePageResponse(items, current, size, total)
	}
}

@Service
class GetMyCapsuleListService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val openRepository: CapsuleOpenRepository,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, pageNumber: Int, size: Int): CapsulePageResponse<CapsuleListItemResponse> {
		val now = LocalDateTime.now()
		val values = capsuleRepository.findByMemberIdAndIsDeletedFalse(memberId)
			.filter { it.expiredAt?.isAfter(now) != false }
			.sortedByDescending { it.createdAt }
			.map { it.toListItem(openRepository.existsByTimeCapsuleIdAndMemberId(requireNotNull(it.id), memberId), includeWriter = false) }
		val (items, current, total) = page(values, pageNumber, size)
		return CapsulePageResponse(items, current, size, total)
	}
}

@Service
class GetReceivedCapsuleListService(
	private val recipientRepository: CapsuleRecipientRepository,
	private val accessPolicy: CapsuleAccessPolicy,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, pageNumber: Int, size: Int): CapsulePageResponse<CapsuleListItemResponse> {
		val now = LocalDateTime.now()
		val values = recipientRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
			.filter { it.timeCapsule.expiredAt?.isAfter(now) != false && !it.timeCapsule.isDeleted && accessPolicy.canAccess(it.timeCapsule, memberId) }
			.map { it.timeCapsule.toListItem(it.hasOpened) }
		val (items, current, total) = page(values, pageNumber, size)
		return CapsulePageResponse(items, current, size, total)
	}
}

@Service
class GetNearbyCapsuleService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val openRepository: CapsuleOpenRepository,
	private val accessPolicy: CapsuleAccessPolicy,
	private val properties: CapsuleProperties,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, latitude: Double, longitude: Double, radius: Double, pageNumber: Int, size: Int): CapsulePageResponse<NearbyCapsuleResponse> {
		if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0 || radius <= 0 || radius > properties.maxNearbyRadiusMeter) {
			throw BusinessException(ErrorCode.INVALID_INPUT)
		}
		val values = capsuleRepository.findNearby(latitude, longitude, radius).mapNotNull { projection ->
			val capsule = capsuleRepository.findByIdAndIsDeletedFalse(projection.capsuleId).orElse(null) ?: return@mapNotNull null
			if (!accessPolicy.canAccess(capsule, memberId)) return@mapNotNull null
			NearbyCapsuleResponse(
				capsuleId = requireNotNull(capsule.id), title = capsule.name,
				latitude = capsule.location.y, longitude = capsule.location.x,
				distance = projection.distance, requiredDistance = capsule.openRadiusMeter.toDouble(),
				openAt = capsule.openAt, lockType = capsule.lockType,
				isOpened = openRepository.existsByTimeCapsuleIdAndMemberId(requireNotNull(capsule.id), memberId),
			)
		}
		val (items, current, total) = page(values, pageNumber, size)
		return CapsulePageResponse(items, current, size, total)
	}
}

@Service
class GetCapsuleDetailService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val openRepository: CapsuleOpenRepository,
	private val accessPolicy: CapsuleAccessPolicy,
) {
	@Transactional(readOnly = true)
	fun execute(memberId: Long, capsuleId: Long): CapsuleDetailResponse {
		val capsule = capsuleRepository.findByIdAndIsDeletedFalse(capsuleId).orElseThrow { CapsuleException() }
		accessPolicy.requireAccessible(capsule, memberId)
		return capsule.toDetail(openRepository.existsByTimeCapsuleIdAndMemberId(capsuleId, memberId))
	}
}

@Service
class DeleteCapsuleService(
	private val capsuleRepository: TimeCapsuleRepository,
) {
	@Transactional
	fun execute(memberId: Long, capsuleId: Long) {
		val capsule = capsuleRepository.findByIdAndIsDeletedFalse(capsuleId).orElseThrow { CapsuleException() }
		if (capsule.member.id != memberId) throw CapsuleException(ErrorCode.CAPSULE_ACCESS_DENIED)
		capsule.isDeleted = true
	}
}
