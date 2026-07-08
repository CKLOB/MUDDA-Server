package team.cklob.mudda.domain.timecapsule.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.timecapsule.domain.entity.CapsuleHistory

interface CapsuleHistoryRepository : JpaRepository<CapsuleHistory, Long>
