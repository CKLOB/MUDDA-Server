package team.cklob.mudda.domain.member.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Long> {
	fun existsByEmail(email: String): Boolean
	fun existsByNickname(nickname: String): Boolean
	fun findByEmail(email: String): Optional<Member>
	fun findByNickname(nickname: String): Optional<Member>
	fun findByOauthProviderAndProviderId(oauthProvider: OAuthProvider, providerId: String): Optional<Member>

	// Friend search: excludes the viewer, withdrawn/not-yet-signed-up members, and anyone blocked in
	// either direction (all filtered in SQL so pagination stays accurate). Ranks an exact nickname match
	// first, a prefix match second, and any other `contains` match last.
	// `keyword` is the trimmed raw keyword (used for the exact-match rank check); `escapedKeyword` is the
	// same keyword with LIKE wildcards (%, _, !) escaped with '!' (used inside LIKE).
	@Query(
		value = """
			SELECT m FROM Member m
			WHERE m.id <> :viewerId
			  AND m.nickname IS NOT NULL
			  AND m.withdrawnAt IS NULL
			  AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :escapedKeyword, '%')) ESCAPE '!'
			  AND NOT EXISTS (
				  SELECT 1 FROM Block b
				  WHERE (b.blocker.id = :viewerId AND b.blocked.id = m.id)
				     OR (b.blocker.id = m.id AND b.blocked.id = :viewerId)
			  )
			ORDER BY
			  CASE
			    WHEN LOWER(m.nickname) = LOWER(:keyword) THEN 0
			    WHEN LOWER(m.nickname) LIKE LOWER(CONCAT(:escapedKeyword, '%')) ESCAPE '!' THEN 1
			    ELSE 2
			  END,
			  m.nickname ASC
		""",
		countQuery = """
			SELECT COUNT(m) FROM Member m
			WHERE m.id <> :viewerId
			  AND m.nickname IS NOT NULL
			  AND m.withdrawnAt IS NULL
			  AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :escapedKeyword, '%')) ESCAPE '!'
			  AND NOT EXISTS (
				  SELECT 1 FROM Block b
				  WHERE (b.blocker.id = :viewerId AND b.blocked.id = m.id)
				     OR (b.blocker.id = m.id AND b.blocked.id = :viewerId)
			  )
		""",
	)
	fun searchSelectableByNickname(
		@Param("viewerId") viewerId: Long,
		@Param("keyword") keyword: String,
		@Param("escapedKeyword") escapedKeyword: String,
		pageable: Pageable,
	): Page<Member>
}
