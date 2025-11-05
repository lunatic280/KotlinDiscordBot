package com.DiscordBot.KotlinDiscordBot

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class SlashCommandListener(private val commands: List<SlashCommand>) : ListenerAdapter() {

    private val commandMap = commands.associateBy { it.name }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val command = commandMap[event.name]
        if (command != null) {
            command.handle(event)
        } else {
            event.reply("Unknown command").setEphemeral(true).queue()
        }
    }
}