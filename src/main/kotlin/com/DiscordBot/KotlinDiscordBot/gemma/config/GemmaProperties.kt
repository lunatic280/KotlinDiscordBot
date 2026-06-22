package com.DiscordBot.KotlinDiscordBot.gemma.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemma")
data class GemmaProperties(
    val apiKey: String = "",
    val baseUrl: String = "https://generativelanguage.googleapis.com"
)