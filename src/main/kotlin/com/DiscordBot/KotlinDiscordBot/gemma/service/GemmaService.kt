package com.DiscordBot.KotlinDiscordBot.gemma.service

import com.DiscordBot.KotlinDiscordBot.gemma.config.GemmaProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import tools.jackson.databind.JsonNode

@Service
class GemmaService(
    webClientBuilder: WebClient.Builder,
    private val properties: GemmaProperties
) {
    companion object {
        private const val PRIMARY_MODEL = "gemma-4-31b-it"
        private const val FALLBACK_MODEL = "gemma-4-26b-it"
    }
    private val log = LoggerFactory.getLogger(GemmaService::class.java)

    private val client: WebClient = webClientBuilder
        .baseUrl("${properties.baseUrl}/")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .build()

    fun generateText(prompt: String): Mono<String> {
        require(properties.apiKey.isNotBlank()) { "API key is required" }
        require(prompt.isNotBlank()) { "Prompt can not be empty." }

        val request = mutableMapOf<String, Any>(
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            ),
            "tools" to listOf(
                mapOf("googleSearch" to emptyMap<String, Any>())
            ),
            "generationConfig" to mapOf(
                "thinkingConfig" to mapOf(
                    "includeThoughts" to false
                )
            )
        )
        properties.systemPrompt.trim()
            .takeIf { it.isNotBlank() }
            ?.let { systemPrompt ->
                request["system_instruction"] = mapOf(
                    "parts" to listOf(
                        mapOf("text" to systemPrompt)
                    )
                )
            }

        return callModel(PRIMARY_MODEL, properties.apiKey, request)
            .onErrorResume(WebClientResponseException.ServiceUnavailable::class.java) { error ->
                log.warn("{} unavailable. Falling back to {}", PRIMARY_MODEL, FALLBACK_MODEL, error)
                callModel(FALLBACK_MODEL, properties.apiKey, request)
            }
    }

    private fun callModel(model: String, apiKey: String, request: Map<String, Any>): Mono<String> {
        return client.post()
            .uri("/v1beta/models/${model}:generateContent")
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { root -> extractText(root) }
    }

    private fun extractText(root: JsonNode): String {
        val parts = root["candidates"]
            ?.get(0)
            ?.get("content")
            ?.get("parts")

        if (parts == null || !parts.isArray) {
            return "Gemma 응답 텍스트를 찾지 못했습니다."
        }

        val answerParts = mutableListOf<String>()

        for (part in parts) {
            val isThought = part["thought"]?.booleanValue() == true
            if (isThought) {
                continue
            }

            val text = part["text"]?.stringValue()?.trim()
            if (!text.isNullOrBlank()) {
                answerParts += text
            }
        }

        return answerParts
            .joinToString("\n")
            .ifBlank { "Gemma 최종 답변을 찾지 못했습니다." }
    }
}
