package com.DiscordBot.KotlinDiscordBot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebConfig {

    // 공통 WebClient를 만들 때 사용할 Builder 빈을 생성하는 함수입니다.
    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()

    // 빗썸 API 기본 URL과 헤더가 설정된 WebClient 빈을 생성하는 함수입니다.
    @Bean
    fun webClient(builder: WebClient.Builder): WebClient =
        builder
        .baseUrl("https://api.bithumb.com")
        .defaultHeader("accept", "application/json")
            .build()

}
