package com.DiscordBot.KotlinDiscordBot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class KotlinDiscordBotApplication

fun main(args: Array<String>) {
	runApplication<KotlinDiscordBotApplication>(*args)
}
