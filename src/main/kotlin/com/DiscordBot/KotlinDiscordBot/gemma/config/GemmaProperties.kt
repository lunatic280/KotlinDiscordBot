package com.DiscordBot.KotlinDiscordBot.gemma.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemma")
data class GemmaProperties(
    val apiKey: String = "",
    val baseUrl: String = "https://generativelanguage.googleapis.com",
    val systemPrompt: String = "너는 Discord 봇에서 동작하는 한국어 AI 어시스턴트입니다. 답변은 간결하고 정확하게 한국어로 작성하세요."
)
