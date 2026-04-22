package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class ResumeCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "resume"
    override val description = "재생 재개"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        if (!musicPlayerService.isPaused(guild)) {
            event.replyEmbeds(errorEmbed("일시정지 상태가 아닙니다.")).setEphemeral(true).queue()
            return
        }
        musicPlayerService.setPaused(guild, false)
        event.replyEmbeds(successEmbed("재개", "▶ 재생을 재개합니다.")).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "재개")
}
