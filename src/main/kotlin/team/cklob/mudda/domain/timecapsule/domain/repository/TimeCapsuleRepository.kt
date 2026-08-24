package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.timecapsule.domain.entity.TimeCapsule
import java.time.LocalDateTime
import java.util.Optional
import jakarta.persistence.LockModeType

interface TimeCapsuleRepository : JpaRepository<TimeCapsule, Long> {
	@EntityGraph(attributePaths = ["member"])
	fun findByMemberIdAndIsDeletedFalse(memberId: Long): List<TimeCapsule>
	@EntityGraph(attributePaths = ["member"])
	fun findAllByIsDeletedFalseOrderByCreatedAtDesc(): List<TimeCapsule>
	fun findByIdAndIsDeletedFalse(id: Long): Optional<TimeCapsule>

	@EntityGraph(attributePaths = ["member"])
	fun findAllByIdIn(ids: Collection<Long>): List<TimeCapsule>

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM TimeCapsule c JOIN FETCH c.member WHERE c.id = :id AND c.isDeleted = false")
	fun findByIdAndIsDeletedFalseForUpdate(@Param("id") id: Long): Optional<TimeCapsule>

	@Query("SELECT COUNT(c) FROM TimeCapsule c WHERE c.member.id = :memberId AND c.isDeleted = false AND (c.expiredAt IS NULL OR c.expiredAt > :now)")
	fun countActiveByMemberId(@Param("memberId") memberId: Long, @Param("now") now: LocalDateTime): Long

	@Query(
		value = """
			SELECT ST_DWithin(
				location::geography,
				ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
				open_radius_meter
			) FROM tbl_time_capsule WHERE id = :capsuleId
		""",
		nativeQuery = true,
	)
	fun isWithinOpeningRadius(
		@Param("capsuleId") capsuleId: Long,
		@Param("latitude") latitude: Double,
		@Param("longitude") longitude: Double,
	): Boolean

	@Query(
		value = """
			SELECT id AS capsuleId,
				ST_Distance(location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS distance
			FROM tbl_time_capsule
			WHERE is_deleted = FALSE
			  AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
			  AND ST_DWithin(location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radius)
			ORDER BY distance, id
		""",
		nativeQuery = true,
	)
	fun findNearby(
		@Param("latitude") latitude: Double,
		@Param("longitude") longitude: Double,
		@Param("radius") radius: Double,
	): List<NearbyCapsuleProjection>
}

interface NearbyCapsuleProjection {
	val capsuleId: Long
	val distance: Double
}
