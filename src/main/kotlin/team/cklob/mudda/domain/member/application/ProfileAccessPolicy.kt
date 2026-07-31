package team.cklob.mudda.domain.member.application

import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility

object ProfileAccessPolicy {
	fun canView(visibility: ProfileVisibility, isSelf: Boolean, friendStatus: FriendStatus): Boolean {
		if (isSelf) return true
		return when (visibility) {
			ProfileVisibility.PUBLIC -> true
			ProfileVisibility.FRIEND -> friendStatus == FriendStatus.FRIEND
			ProfileVisibility.PRIVATE -> false
		}
	}
}
