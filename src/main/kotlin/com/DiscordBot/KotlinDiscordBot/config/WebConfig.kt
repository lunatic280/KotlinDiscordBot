package com.DiscordBot.KotlinDiscordBot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebConfig {

    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()

    @Bean
    fun webClient(builder: WebClient.Builder): WebClient =
        builder
        .baseUrl("https://api.bithumb.com")
        .defaultHeader("accept", "application/json")
            .build()

}
