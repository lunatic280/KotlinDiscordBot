package com.DiscordBot.KotlinDiscordBot.gemma.service

import com.DiscordBot.KotlinDiscordBot.gemma.config.GemmaProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.Locale

@Service
class GemmaService(
    webClientBuilder: WebClient.Builder,
    private val properties: GemmaProperties,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val PRIMARY_MODEL = "gemma-4-31b-it"
        private const val FALLBACK_MODEL = "gemma-4-26b-a4b-it"
    }
    private val log = LoggerFactory.getLogger(GemmaService::class.java)

    private val client: WebClient = webClientBuilder
        .baseUrl("${properties.baseUrl}/")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .build()

    fun generateText(prompt: String, requestId: String): Mono<GemmaResult> {
        return Mono.defer {
            val serviceStartedAt = System.nanoTime()

            require(properties.apiKey.isNotBlank()) { "API key is required" }
            require(prompt.isNotBlank()) { "Prompt can not be empty." }

            val requestBuildStartedAt = System.nanoTime()
            val request = mutableMapOf<String, Any>(
                "contents" to listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(
                            mapOf("text" to prompt)
                        )
                    )
                ),
//                "tools" to listOf(
//                    mapOf("googleSearch" to emptyMap<String, Any>())
//                ),
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
            val requestBuildMs = elapsedMs(requestBuildStartedAt)
            val requestJsonSerializeStartedAt = System.nanoTime()
            val requestJson = objectMapper.writeValueAsString(request)
            val requestJsonSerializeMs = elapsedMs(requestJsonSerializeStartedAt)
            val modelCalls = mutableListOf<GemmaModelCallTiming>()

            callModel(PRIMARY_MODEL, properties.apiKey, requestJson, requestId, modelCalls)
                .onErrorResume(WebClientResponseException.ServiceUnavailable::class.java) { error ->
                    log.warn(
                        "[{}] {} unavailable. Falling back to {}",
                        requestId,
                        PRIMARY_MODEL,
                        FALLBACK_MODEL,
                        error,
                    )
                    callModel(FALLBACK_MODEL, properties.apiKey, requestJson, requestId, modelCalls)
                }
                .map { root ->
                    val extractStartedAt = System.nanoTime()
                    val answer = extractText(root)
                    val responseExtractMs = elapsedMs(extractStartedAt)

                    GemmaResult(
                        answer = answer,
                        timing = GemmaTiming(
                            requestBuildMs = requestBuildMs,
                            requestJsonSerializeMs = requestJsonSerializeMs,
                            modelCalls = modelCalls.toList(),
                            responseExtractMs = responseExtractMs,
                            serviceTotalMs = elapsedMs(serviceStartedAt),
                        ),
                    )
                }
                .doOnError {
                    log.info(
                        "[{}] Gemma service failed. total={}ms, modelCalls={}",
                        requestId,
                        formatMs(elapsedMs(serviceStartedAt)),
                        modelCalls.joinToString { call -> call.summary() },
                    )
                }
        }
    }

    private fun callModel(
        model: String,
        apiKey: String,
        requestJson: String,
        requestId: String,
        modelCalls: MutableList<GemmaModelCallTiming>,
    ): Mono<JsonNode> {
        return Mono.defer {
            val modelCallStartedAt = System.nanoTime()
            var httpAndBodyMs = 0.0
            var responseJsonParseMs = 0.0

            client.post()
                .uri("/v1beta/models/${model}:generateContent")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String::class.java)
                .map { responseBody ->
                    httpAndBodyMs = elapsedMs(modelCallStartedAt)
                    val responseJsonParseStartedAt = System.nanoTime()
                    val root = objectMapper.readTree(responseBody)
                    responseJsonParseMs = elapsedMs(responseJsonParseStartedAt)
                    root
                }
                .doOnSuccess {
                    val timing = GemmaModelCallTiming(
                        model = model,
                        httpAndBodyMs = httpAndBodyMs,
                        responseJsonParseMs = responseJsonParseMs,
                        elapsedMs = elapsedMs(modelCallStartedAt),
                        succeeded = true,
                    )
                    modelCalls += timing
                    log.info("[{}] Gemma model call completed: {}", requestId, timing.summary())
                }
                .doOnError { error ->
                    val statusCode = (error as? WebClientResponseException)?.statusCode?.value()
                    val timing = GemmaModelCallTiming(
                        model = model,
                        httpAndBodyMs = httpAndBodyMs,
                        responseJsonParseMs = responseJsonParseMs,
                        elapsedMs = elapsedMs(modelCallStartedAt),
                        succeeded = false,
                        statusCode = statusCode,
                    )
                    modelCalls += timing
                    log.info("[{}] Gemma model call failed: {}", requestId, timing.summary())
                }
        }
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

    private fun GemmaModelCallTiming.summary(): String {
        val result = if (succeeded) "success" else "failed"
        val status = statusCode?.let { ", status=$it" }.orEmpty()
        return "$model=${formatMs(elapsedMs)}ms " +
            "(httpAndBody=${formatMs(httpAndBodyMs)}ms, " +
            "jsonParse=${formatMs(responseJsonParseMs)}ms, $result$status)"
    }

    private fun elapsedMs(startedAt: Long): Double =
        (System.nanoTime() - startedAt) / 1_000_000.0

    private fun formatMs(value: Double): String =
        String.format(Locale.US, "%.3f", value)
}
