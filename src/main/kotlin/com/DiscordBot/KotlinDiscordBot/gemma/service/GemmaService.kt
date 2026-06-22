package com.DiscordBot.KotlinDiscordBot.gemma.service

import com.DiscordBot.KotlinDiscordBot.gemma.config.GemmaProperties
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

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
        return root["candidates"]
            ?.get(0)
            ?.get("content")
            ?.get("parts")
            ?.get(0)
            ?.get("text")
            ?.asText()
            ?: "Gemma 응답 텍스트를 찾지 못했습니다."
    }


}
