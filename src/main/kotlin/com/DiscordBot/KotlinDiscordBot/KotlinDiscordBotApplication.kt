package com.DiscordBot.KotlinDiscordBot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class KotlinDiscordBotApplication

// Spring Boot Discord bot 애플리케이션을 시작하는 진입점 함수입니다.
fun main(args: Array<String>) {
	runApplication<KotlinDiscordBotApplication>(*args)
}
