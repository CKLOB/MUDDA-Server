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
	//
	// This does a leading-wildcard LIKE ('%keyword%'), so it cannot use a B-tree (or even a LOWER()
	// functional) index and always scans tbl_member in full -- fine at today's member counts, but if
	// nickname search traffic or table size grows enough for this to show up in query latency, switch to
	// a trigram index instead: `CREATE EXTENSION pg_trgm;
	// CREATE INDEX idx_member_nickname_trgm ON tbl_member USING gin (LOWER(nickname) gin_trgm_ops);`
	//
	// `keyword` is the trimmed raw keyword (used for the exact-match rank check); `escapedKeyword` is the
	// same keyword with LIKE wildcards (%, _, !) escaped with '!' (used inside LIKE). Callers should use
	// the 3-arg overload below instead of calling this one directly.
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

	// Escapes LIKE wildcards (%, _) and the escape character itself ('!') so a keyword containing them is
	// matched literally instead of as a wildcard pattern, then delegates to the 4-arg query above (paired
	// with its `ESCAPE '!'` clauses). Keeping this in the repository -- rather than in the calling service
	// -- means the escaping rule and the ESCAPE clause it must match live in the same file, and a future
	// caller can't forget to escape before calling.
	fun searchSelectableByNickname(viewerId: Long, keyword: String, pageable: Pageable): Page<Member> {
		val escapedKeyword = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_")
		return searchSelectableByNickname(viewerId, keyword, escapedKeyword, pageable)
	}
}
