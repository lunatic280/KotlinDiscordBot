package com.DiscordBot.KotlinDiscordBot.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

interface SlashCommand {
    val name: String
    val description: String
    fun handle(event: SlashCommandInteractionEvent)
    fun getCommandData(): SlashCommandData
}