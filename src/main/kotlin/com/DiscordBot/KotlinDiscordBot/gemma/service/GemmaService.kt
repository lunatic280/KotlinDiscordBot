package com.DiscordBot.KotlinDiscordBot.gemma.service

import com.DiscordBot.KotlinDiscordBot.gemma.config.GemmaProperties
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class GemmaService(
    webClientBuilder: WebClient.Builder,
    private val properties: GemmaProperties
) {
    companion object {
        private const val MODEL = "gemma-4-31b-it"
    }
    private val log = LoggerFactory.getLogger(GemmaService::class.java)

    private val client: WebClient = webClientBuilder
        .baseUrl("${properties.baseUrl}/")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .build()

    fun generateText(prompt: String): Mono<String> {
        if (properties.apiKey.isNotEmpty()) {
            throw IllegalArgumentException("API key is required")
        }
        if (prompt.isNotEmpty()) {
            throw IllegalArgumentException("Prompt can not be empty.")
        }

        val request = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )
        )

        return client.post()
            .uri("/v1beta/models/${MODEL}:generateContent")
            .header("x-goog-api-key", properties.apiKey)
            .header("Content-Type", "application/json")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { root ->
                root["candidates"]
                    ?.get(0)
                    ?.get("content")
                    ?.get("parts")
                    ?.get(0)
                    ?.get("text")
                    ?.asText()
                    ?: "Gemma 응답 텍스트를 찾지 못했습니다."
            }
    }
}
