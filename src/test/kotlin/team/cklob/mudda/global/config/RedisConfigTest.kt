package team.cklob.mudda.global.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.mockk.mockk
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializer

class RedisConfigTest {
    @Test fun `serializes Kotlin data classes without field loss`() {
        val serializer = RedisConfig().redisTemplate(mockk<RedisConnectionFactory>(), com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()).valueSerializer as RedisSerializer<Any>
        val value = CachedValue(1, "capsule")

        assertEquals(value, serializer.deserialize(serializer.serialize(value)))
    }

    private data class CachedValue(val id: Long, val name: String)
}
