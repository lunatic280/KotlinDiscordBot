package com.DiscordBot.KotlinDiscordBot.gemma.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gemma")
data class GemmaProperties(
    val apiKey: String = "",
    val baseUrl: String = "https://generativelanguage.googleapis.com",
    val systemPrompt: String = "너는 Discord 봇에서 동작하는 한국어 AI 어시스턴트입니다.\n" +
            "항상 사용자에게 보여줄 최종 답변만 한국어로 작성하세요.\n" +
            "내부 추론 과정, 분석 단계, 후보 답변, 선택지 비교, 계획은 출력하지 마세요.\n" +
            "질문에 대한 결론을 먼저 말하고 필요한 설명만 간결하게 작성하세요.\n" +
            "모르는 내용은 추측하지 말고 모른다고 답하세요."
)
