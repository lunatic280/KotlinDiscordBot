package com.DiscordBot.KotlinDiscordBot.config

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.SlashCommandListener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JdaConfig(
    @Value("\${discord.token}") private val token: String
) {

    @Bean
    fun jda(slashListener: SlashCommandListener, commands: List<SlashCommand>): JDA {
        val jda = JDABuilder.createDefault(token)
            .setActivity(Activity.playing("Type /ping"))
            .addEventListeners(slashListener)
            .addEventListeners(object : ListenerAdapter() {
                override fun onReady(event: ReadyEvent) {
                    val commandData: List<SlashCommandData> = commands.map {
                        it.getCommandData()
                    }
                    event.jda.updateCommands().addCommands(commandData).queue()
                }
            })
            .build()
        return jda
    }
}