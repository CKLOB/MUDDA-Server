package team.cklob.mudda.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import team.cklob.mudda.global.security.LoginUserArgumentResolver

@Configuration
class WebConfig(private val loginUserArgumentResolver: LoginUserArgumentResolver) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) { resolvers += loginUserArgumentResolver }
}
