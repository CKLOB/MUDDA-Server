package team.cklob.mudda.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
	@Bean
	fun openAPI(): OpenAPI = OpenAPI()
		.info(
			Info()
				.title("MUDDA API")
				.version("v1")
				.description(
					"""
					위치 잠금 타임캡슐 서비스 API입니다.

					**인증** — `/api/v1/auth/oauth/{provider}` 로 발급받은 액세스 토큰을 우측 상단 Authorize 에 입력하면
					인증이 필요한 API를 그대로 호출할 수 있습니다. `auth/oauth`, `auth/reissue` 외의 모든 엔드포인트는 인증이 필요합니다.

					**응답 형식** — 모든 응답은 `{ "success": true, "data": ... }` 또는
					`{ "success": false, "error": { "code": "...", "message": "..." } }` 형태로 감싸집니다.
					""".trimIndent(),
				),
		)
		.components(
			Components().addSecuritySchemes(
				"bearerAuth",
				SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
					.description("발급받은 액세스 토큰. `Bearer ` 접두사는 자동으로 붙습니다."),
			),
		)
}
