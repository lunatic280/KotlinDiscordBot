package com.DiscordBot.KotlinDiscordBot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KotlinDiscordBotApplication

fun main(args: Array<String>) {
	runApplication<KotlinDiscordBotApplication>(*args)
}
