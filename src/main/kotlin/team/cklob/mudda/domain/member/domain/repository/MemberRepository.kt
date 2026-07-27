package team.cklob.mudda.domain.member.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import team.cklob.mudda.domain.member.domain.entity.Member
import team.cklob.mudda.domain.member.domain.type.OAuthProvider
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Long> {
	fun existsByEmail(email: String): Boolean
	fun existsByNickname(nickname: String): Boolean
	fun findByEmail(email: String): Optional<Member>
	fun findByNickname(nickname: String): Optional<Member>
	fun findByOauthProviderAndProviderIdAndWithdrawnAtIsNull(oauthProvider: OAuthProvider, providerId: String): Optional<Member>
	fun existsByOauthProviderAndProviderIdAndWithdrawnAtIsNotNull(oauthProvider: OAuthProvider, providerId: String): Boolean
}
