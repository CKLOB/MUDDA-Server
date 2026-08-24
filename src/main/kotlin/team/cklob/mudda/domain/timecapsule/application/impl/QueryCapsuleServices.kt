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
		// ponytail: filtering remains in memory, but authorization and open state are fetched in fixed-size
		// batches. Move the whole predicate into a paged query when scanning all capsules becomes measurable.
		val active = capsuleRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()
			.filter { it.expiredAt?.isAfter(now) != false }
		val accessible = accessPolicy.filterAccessible(active, memberId)
		val openedIds = openedIds(openRepository, memberId, accessible)
		val visible = accessible.map { it.toListItem(requireNotNull(it.id) in openedIds) }
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
		val openedIds = openedIds(openRepository, memberId, values)
		val responses = values.map { it.toListItem(requireNotNull(it.id) in openedIds, includeWriter = false) }
		val (items, current, total) = page(responses, pageNumber, size)
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
		val recipients = recipientRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
			.filter { it.timeCapsule.expiredAt?.isAfter(now) != false && !it.timeCapsule.isDeleted }
		val accessibleIds = accessPolicy.filterAccessible(recipients.map { it.timeCapsule }, memberId)
			.mapTo(mutableSetOf()) { requireNotNull(it.id) }
		val values = recipients.filter { requireNotNull(it.timeCapsule.id) in accessibleIds }
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
		val nearby = capsuleRepository.findNearby(latitude, longitude, radius)
		val capsulesById = capsuleRepository.findAllByIdIn(nearby.map { it.capsuleId })
			.associateBy { requireNotNull(it.id) }
		val accessibleIds = accessPolicy.filterAccessible(capsulesById.values.toList(), memberId)
			.mapTo(mutableSetOf()) { requireNotNull(it.id) }
		val openedIds = accessibleIds.takeIf { it.isNotEmpty() }
			?.let { openRepository.findOpenedCapsuleIds(memberId, it) }.orEmpty()
		val values = nearby.mapNotNull { projection ->
			val capsule = capsulesById[projection.capsuleId]
				?.takeIf { requireNotNull(it.id) in accessibleIds } ?: return@mapNotNull null
			NearbyCapsuleResponse(
				capsuleId = requireNotNull(capsule.id), title = capsule.name,
				latitude = capsule.location.y, longitude = capsule.location.x,
				distance = projection.distance, requiredDistance = capsule.openRadiusMeter.toDouble(),
				openAt = capsule.openAt, lockType = capsule.lockType,
				isOpened = requireNotNull(capsule.id) in openedIds,
			)
		}
		val (items, current, total) = page(values, pageNumber, size)
		return CapsulePageResponse(items, current, size, total)
	}
}

private fun openedIds(openRepository: CapsuleOpenRepository, memberId: Long, capsules: List<TimeCapsule>): Set<Long> =
	capsules.map { requireNotNull(it.id) }.takeIf { it.isNotEmpty() }
		?.let { openRepository.findOpenedCapsuleIds(memberId, it) }.orEmpty()

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
