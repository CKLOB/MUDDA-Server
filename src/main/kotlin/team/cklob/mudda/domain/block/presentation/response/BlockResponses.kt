package team.cklob.mudda.domain.block.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import team.cklob.mudda.domain.block.domain.entity.Block
import java.time.LocalDateTime

@Schema(description = "차단한 회원")
data class BlockResponse(
	@Schema(description = "차단 ID", example = "1")
	val blockId: Long,

	@Schema(description = "차단된 회원 ID", example = "2")
	val memberId: Long,

	@Schema(description = "차단된 회원 닉네임", example = "nick", nullable = true)
	val nickname: String?,

	@Schema(description = "프로필 이미지 URL", nullable = true)
	val profileImageUrl: String?,

	@Schema(description = "차단 시각")
	val createdAt: LocalDateTime,
) {
	companion object {
		fun from(block: Block) = BlockResponse(
			blockId = requireNotNull(block.id),
			memberId = requireNotNull(block.blocked.id),
			nickname = block.blocked.nickname,
			profileImageUrl = block.blocked.profileImageUrl,
			createdAt = block.createdAt,
		)
	}
}

@Schema(description = "회원 차단 응답")
data class CreateBlockResponse(
	@Schema(description = "차단 ID", example = "1")
	val blockId: Long,

	@Schema(description = "차단된 회원 ID", example = "2")
	val memberId: Long,

	@Schema(description = "차단 시각")
	val createdAt: LocalDateTime,
)
