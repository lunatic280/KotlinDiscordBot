package com.DiscordBot.KotlinDiscordBot.gemma.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@EnableConfigurationProperties(GemmaProperties::class)
class GemmaConfig {
}