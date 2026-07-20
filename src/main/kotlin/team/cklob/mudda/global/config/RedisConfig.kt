package team.cklob.mudda.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {
    @Bean
    fun redisTemplate(factory: RedisConnectionFactory, objectMapper: ObjectMapper) = RedisTemplate<String, Any>().apply {
        connectionFactory = factory
        keySerializer = StringRedisSerializer()
        valueSerializer = GenericJackson2JsonRedisSerializer(objectMapper.copy().activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("team.cklob.mudda")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .build(),
            ObjectMapper.DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.PROPERTY,
        ))
    }
}
