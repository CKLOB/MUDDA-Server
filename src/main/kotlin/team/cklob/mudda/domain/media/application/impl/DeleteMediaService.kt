package team.cklob.mudda.domain.media.application.impl

import org.springframework.stereotype.Service
import team.cklob.mudda.domain.media.application.MediaStorage
import team.cklob.mudda.domain.media.domain.repository.MediaRepository
import team.cklob.mudda.global.exception.BusinessException
import team.cklob.mudda.global.exception.ErrorCode

@Service
class DeleteMediaService(
	private val mediaRepository: MediaRepository,
	private val mediaStorage: MediaStorage,
) {
	fun execute(memberId: Long, mediaId: Long) {
		val media = mediaRepository.findByIdAndUploaderId(mediaId, memberId)
			?: throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)
		if (media.timeCapsule != null) throw BusinessException(ErrorCode.MEDIA_ALREADY_ATTACHED)

		mediaRepository.delete(media)
		mediaStorage.delete(media.s3Key)
	}
}
