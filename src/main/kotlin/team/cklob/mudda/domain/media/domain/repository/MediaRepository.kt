package team.cklob.mudda.domain.media.domain.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import team.cklob.mudda.domain.media.domain.entity.Media
import java.time.LocalDateTime

interface MediaRepository : JpaRepository<Media, Long> {
	fun findByS3KeyAndUploaderId(s3Key: String, uploaderId: Long): Media?
	fun findByIdAndUploaderId(id: Long, uploaderId: Long): Media?
	fun findAllByTimeCapsuleId(timeCapsuleId: Long): List<Media>

	// Media registered through the upload-complete flow but never attached to a capsule. V4 made
	// time_capsule_id nullable to allow that intermediate state, which means an abandoned compose leaves
	// both the row and its S3 object behind forever.
	@Query("SELECT m FROM Media m WHERE m.timeCapsule IS NULL AND m.createdAt < :threshold")
	fun findUnattachedOlderThan(@Param("threshold") threshold: LocalDateTime, pageable: Pageable): List<Media>

	@Modifying
	@Transactional
	@Query(
		value = """
			INSERT INTO tbl_media (uploader_id, media_type, s3_key, created_at)
			VALUES (:uploaderId, :mediaType, :s3Key, CURRENT_TIMESTAMP)
			ON CONFLICT (s3_key) DO NOTHING
		""",
		nativeQuery = true,
	)
	fun insertUnattached(
		@Param("uploaderId") uploaderId: Long,
		@Param("mediaType") mediaType: String,
		@Param("s3Key") s3Key: String,
	): Int
}
