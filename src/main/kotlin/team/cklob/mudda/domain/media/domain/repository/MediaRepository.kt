package team.cklob.mudda.domain.media.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.media.domain.entity.Media

interface MediaRepository : JpaRepository<Media, Long>
