package team.cklob.mudda.domain.member.application

import team.cklob.mudda.domain.friend.domain.type.FriendStatus
import team.cklob.mudda.domain.member.domain.type.ProfileVisibility

object ProfileAccessPolicy {
	fun canView(visibility: ProfileVisibility, isSelf: Boolean, friendStatus: FriendStatus): Boolean = when {
		isSelf -> true
		visibility == ProfileVisibility.PUBLIC -> true
		visibility == ProfileVisibility.FRIEND -> friendStatus == FriendStatus.FRIEND
		else -> false
	}
}
