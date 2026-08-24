package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.timecapsule.domain.entity.Guestbook

interface GuestbookRepository : JpaRepository<Guestbook, Long> {
	fun findByTimeCapsuleIdAndIsDeletedFalseOrderByCreatedAtDesc(timeCapsuleId: Long): List<Guestbook>
	fun findByIdAndTimeCapsuleIdAndIsDeletedFalse(id: Long, timeCapsuleId: Long): Guestbook?
}
