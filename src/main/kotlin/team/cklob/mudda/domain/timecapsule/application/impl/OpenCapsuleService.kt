package team.cklob.mudda.domain.timecapsule.application.impl

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.domain.feed.infrastructure.FeedBroadcaster
import team.cklob.mudda.domain.feed.presentation.response.FeedResponse
import team.cklob.mudda.domain.member.domain.repository.MemberRepository
import team.cklob.mudda.domain.notification.application.impl.NotificationPublisher
import team.cklob.mudda.domain.notification.domain.type.NotificationTargetType
import team.cklob.mudda.domain.notification.domain.type.NotificationType
import team.cklob.mudda.domain.timecapsule.application.CapsuleAccessPolicy
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleOpen
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleOpenRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.KeyShareRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.CapsuleRecipientRepository
import team.cklob.mudda.domain.timecapsule.domain.repository.TimeCapsuleRepository
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleEncryptionMode
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleLockType
import team.cklob.mudda.domain.timecapsule.domain.type.CapsuleVisibility
import team.cklob.mudda.domain.timecapsule.presentation.request.OpenCapsuleRequest
import team.cklob.mudda.domain.timecapsule.presentation.response.KeyShareResponse
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
	private val accessPolicy: CapsuleAccessPolicy,
	private val notificationPublisher: NotificationPublisher,
	private val feedBroadcaster: FeedBroadcaster,
	private val keyShareRepository: KeyShareRepository,
) {
	@Transactional
	fun execute(memberId: Long, capsuleId: Long, request: OpenCapsuleRequest): OpenCapsuleResponse {
		// ponytail: a capsule-row lock is intentionally coarse; split locking per member only if concurrent
		// first-open throughput for the same capsule becomes measurable.
		val capsule = capsuleRepository.findByIdAndIsDeletedFalseForUpdate(capsuleId).orElseThrow { CapsuleException() }
		val now = LocalDateTime.now()
		accessPolicy.requireAccessible(capsule, memberId, now)
		if (capsule.openAt.isAfter(now)) throw CapsuleException(ErrorCode.CAPSULE_NOT_OPEN_YET)
		if (!capsuleRepository.isWithinOpeningRadius(capsuleId, request.latitude, request.longitude)) {
			throw CapsuleException(ErrorCode.CAPSULE_OUT_OF_RANGE)
		}
		var opened = openRepository.findByTimeCapsuleIdAndMemberId(capsuleId, memberId).orElse(null)
		if (opened == null) {
			// No lock check here on purpose. The server verifies location and access only; proving knowledge
			// of the lock secret happens on the client, when it unwraps its key share. Verifying server-side
			// would mean receiving the secret in plaintext, and a server that has seen it can derive the same
			// wrapping key and open every capsule it stores -- which is exactly the guarantee CLIENT_E2E is
			// supposed to provide. A wrong secret fails the share's GCM tag check instead, which is strictly
			// stronger than a bcrypt comparison: it cannot be bypassed by anything the server does.
			val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }
			val openLocation = GeometryFactory(PrecisionModel(), 4326).createPoint(Coordinate(request.longitude, request.latitude))
			opened = openRepository.save(CapsuleOpen(capsule, member, now, openLocation))
			recipientRepository.findByTimeCapsuleIdAndMemberId(capsuleId, memberId).ifPresent {
				it.hasOpened = true
				it.openedAt = now
			}
			announceFirstOpen(capsule, opened)
		}
		val media = mediaRepository.findAllByTimeCapsuleId(capsuleId).map {
			MediaResponse(requireNotNull(it.id), mediaStorage.createAccessUrl(it.s3Key).url, it.mediaType)
		}
		// The lock has been verified by this point, which is what gates release of the server's shares. For a
		// CLIENT_E2E capsule the server hands back its sub-threshold shares and the blob and stops there --
		// it has no key to decrypt with, and returning `content` would be a lie about what it holds.
		val e2e = capsule.encryptionMode == CapsuleEncryptionMode.CLIENT_E2E
		val shares = if (e2e) {
			keyShareRepository.findAllByTimeCapsuleIdOrderByShareIndex(capsuleId)
				.map { KeyShareResponse(it.shareIndex, it.shareData, it.isWrapped) }
		} else {
			emptyList()
		}
		return OpenCapsuleResponse(
			capsuleId = capsuleId,
			title = capsule.name,
			encryptionMode = capsule.encryptionMode,
			content = capsule.content.takeUnless { e2e },
			contentCipher = capsule.content.takeIf { e2e },
			keyShares = shares,
			keyThreshold = capsule.keyThreshold,
			writer = writer(capsule),
			media = media,
			openedAt = opened.openedAt,
		)
	}

	// Only the first open is newsworthy: re-opening a capsule you already unlocked must not notify the
	// owner again or repost to the feed.
	private fun announceFirstOpen(capsule: TimeCapsule, opened: CapsuleOpen) {
		val opener = opened.member
		val capsuleId = requireNotNull(capsule.id)
		if (capsule.member.id != opener.id) {
			notificationPublisher.publish(
				recipient = capsule.member,
				type = NotificationType.CAPSULE_OPENED,
				title = "캡슐이 열렸어요",
				content = "${opener.nickname ?: "누군가"}님이 '${capsule.name.shortenForNotification()}'을(를) 열었어요.",
				targetId = capsuleId,
				targetType = NotificationTargetType.CAPSULE,
			)
		}
		if (capsule.visibility == CapsuleVisibility.PUBLIC) {
			feedBroadcaster.broadcast(FeedResponse.from(opened))
		}
	}
}
