package com.DiscordBot.KotlinDiscordBot.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class PingCommand: SlashCommand {
    override val name = "ping"
    override val description = "퐁!"
    override fun handle(event: SlashCommandInteractionEvent) {
        event.reply("Pong!").queue()
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
    }
}