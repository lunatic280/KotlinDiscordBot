package com.DiscordBot.KotlinDiscordBot.coin

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class CoinService(
    private val webClient: WebClient
) {

    private val log = LoggerFactory.getLogger(CoinService::class.java)

    fun getCoin(market: Market): String? {
        return runCatching {
            webClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/v1/ticker")
                        .queryParam("markets", market.code)
                        .build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        }.onFailure { exception -> log.warn("문제가 생겼어요.") }
            .getOrNull()
    }

}