package team.cklob.mudda.domain.media.infrastructure

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MediaStorageProperties::class)
class MediaStorageConfig
