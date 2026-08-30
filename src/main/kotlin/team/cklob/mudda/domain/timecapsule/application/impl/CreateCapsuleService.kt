package team.cklob.mudda.domain.timecapsule.application.impl

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.block.domain.repository.BlockRepository
import team.cklob.mudda.domain.friend.domain.repository.FriendRepository
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.notification.application.impl.NotificationPublisher
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.domain.timecapsule.application.CapsuleEncryptionPolicy
import team.cklob.mudda.domain.timecapsule.application.CapsuleProperties
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleRecipient
import team.cklob.mudda.domain.timecapsule.domain.entity.KeyShare
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleRecipientRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.KeyShareRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleEncryptionMode
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.presentation.request.CreateCapsuleRequest
import team.cklob.mudda.domain.timecapsule.presentation.response.CreateCapsuleResponse
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode
import java.time.LocalDateTime

@Service
class CreateCapsuleService(
	private val capsuleRepository: TimeCapsuleRepository,
	private val recipientRepository: CapsuleRecipientRepository,
	private val memberRepository: MemberRepository,
	private val friendRepository: FriendRepository,
	private val blockRepository: BlockRepository,
	private val mediaRepository: MediaRepository,
	private val passwordEncoder: PasswordEncoder,
	private val properties: CapsuleProperties,
	private val notificationPublisher: NotificationPublisher,
	private val keyShareRepository: KeyShareRepository,
	private val encryptionPolicy: CapsuleEncryptionPolicy,
) {
	@Transactional
	fun execute(memberId: Long, request: CreateCapsuleRequest): CreateCapsuleResponse {
		val now = LocalDateTime.now()
		validate(request, now)
		val encryptionMode = encryptionPolicy.resolveMode(request.lockType)
		encryptionPolicy.validate(request, encryptionMode)
		if (capsuleRepository.countActiveByMemberId(memberId, now) >= properties.maxActivePerMember) {
			throw BusinessException(ErrorCode.CAPSULE_LIMIT_EXCEEDED)
		}
		val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
		val recipients = memberRepository.findAllById(request.recipientIds).associateBy { requireNotNull(it.id) }
		if (recipients.size != request.recipientIds.size || request.recipientIds.any { !validRecipient(memberId, it) }) {
			throw BusinessException(ErrorCode.INVALID_CAPSULE_RECIPIENT)
		}
		val media = mediaRepository.findAllByIdForUpdate(request.mediaIds)
		if (media.size != request.mediaIds.size || media.any { it.uploader.id != memberId || it.timeCapsule != null }) {
			throw BusinessException(ErrorCode.INVALID_CAPSULE_MEDIA)
		}
		val location = GeometryFactory(PrecisionModel(), 4326).createPoint(Coordinate(request.longitude, request.latitude))
		val capsule = capsuleRepository.save(
			TimeCapsule(
				member = member,
				name = request.name.trim(),
				// Exactly one of the two is set, enforced by CapsuleEncryptionPolicy. For CLIENT_E2E the
				// stored value is the client's ciphertext, which the server cannot open.
				content = request.content ?: request.contentCipher,
				encryptionMode = encryptionMode,
				keyThreshold = request.keyThreshold,
				visibility = request.visibility,
				lockType = request.lockType,
				passwordHash = request.password?.let(passwordEncoder::encode),
				question = request.question?.trim(),
				answerHash = request.answer?.trim()?.lowercase()?.let(passwordEncoder::encode),
				location = location,
				openRadiusMeter = properties.openRadiusMeter,
				openAt = request.openAt,
				expiredAt = request.expiredAt,
			),
		)
		if (encryptionMode == CapsuleEncryptionMode.CLIENT_E2E) {
			keyShareRepository.saveAll(
				request.keyShares.orEmpty().map {
					KeyShare(capsule, requireNotNull(it.index), requireNotNull(it.data), requireNotNull(it.isWrapped))
				},
			)
		}
		recipientRepository.saveAll(recipients.values.map { CapsuleRecipient(it, capsule) })
		media.forEach { it.timeCapsule = capsule }
		// Recipients are told a capsule is waiting for them, but not where or what is in it -- the whole
		// point is that they have to go find it.
		recipients.values.forEach {
			notificationPublisher.publish(
				recipient = it,
				type = NotificationType.CAPSULE_RECEIVED,
				title = "새로운 캡슐이 도착했어요",
				content = "${member.nickname ?: "누군가"}님이 '${capsule.name.shortenForNotification()}'을(를) 남겼어요.",
				targetId = requireNotNull(capsule.id),
				targetType = NotificationTargetType.CAPSULE,
			)
		}
		return CreateCapsuleResponse(requireNotNull(capsule.id), capsule.name, location.y, location.x, capsule.openAt, capsule.expiredAt, capsule.createdAt)
	}

	private fun validRecipient(memberId: Long, recipientId: Long): Boolean = recipientId != memberId &&
		friendRepository.existsAcceptedBetween(memberId, recipientId) &&
		!blockRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(memberId, recipientId, recipientId, memberId)

	private fun validate(request: CreateCapsuleRequest, now: LocalDateTime) {
		if (request.openAt.isBefore(now) || request.expiredAt?.let { !it.isAfter(request.openAt) || it.isAfter(request.openAt.plusYears(properties.maxExpirationYears)) } == true) {
			throw BusinessException(ErrorCode.INVALID_INPUT)
		}
		val validLock = when (request.lockType) {
			CapsuleLockType.NONE -> request.password == null && request.question == null && request.answer == null
			CapsuleLockType.PASSWORD -> !request.password.isNullOrBlank() && request.question == null && request.answer == null
			CapsuleLockType.QUESTION -> request.password == null && !request.question.isNullOrBlank() && !request.answer.isNullOrBlank()
		}
		if (!validLock) throw BusinessException(ErrorCode.INVALID_INPUT)
	}
}
