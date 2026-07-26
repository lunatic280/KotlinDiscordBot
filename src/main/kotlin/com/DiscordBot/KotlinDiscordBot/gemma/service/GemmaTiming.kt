package com.DiscordBot.KotlinDiscordBot.gemma.service

data class GemmaModelCallTiming(
    val model: String,
    val httpAndBodyMs: Double,
    val responseJsonParseMs: Double,
    val elapsedMs: Double,
    val succeeded: Boolean,
    val statusCode: Int? = null,
)

data class GemmaTiming(
    val requestBuildMs: Double,
    val requestJsonSerializeMs: Double,
    val modelCalls: List<GemmaModelCallTiming>,
    val responseExtractMs: Double,
    val serviceTotalMs: Double,
)

data class GemmaResult(
    val answer: String,
    val timing: GemmaTiming,
)
