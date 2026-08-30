package team.cklob.mudda.domain.media.domain.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
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
	//
	// Locked because the cleanup job deletes the S3 object after this read: without the lock a capsule
	// creation could attach one of these rows in between, and the job would then destroy media a live
	// capsule points at. Under PostgreSQL's read-committed isolation, FOR UPDATE re-evaluates the WHERE
	// clause once the lock is granted, so a row attached by a transaction that committed while we waited
	// drops out of the result instead of being returned as still-unattached.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT m FROM Media m WHERE m.timeCapsule IS NULL AND m.createdAt < :threshold")
	fun findUnattachedOlderThan(@Param("threshold") threshold: LocalDateTime, pageable: Pageable): List<Media>

	// The attach side of the same lock: CreateCapsuleService takes it before pointing media at a capsule,
	// so an in-flight cleanup finishes first and the attach then correctly fails validation on the
	// now-deleted row rather than resurrecting it.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT m FROM Media m WHERE m.id IN :ids")
	fun findAllByIdForUpdate(@Param("ids") ids: Collection<Long>): List<Media>

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
