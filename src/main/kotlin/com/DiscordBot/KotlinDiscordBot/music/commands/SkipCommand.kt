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
class SkipCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "skip"
    override val description = "현재 곡 스킵"

    // 현재 재생 중인 곡을 건너뛰는 명령 처리 함수입니다.
    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val skipped = musicPlayerService.skip(guild)
        if (skipped == null) {
            event.replyEmbeds(errorEmbed("현재 재생 중인 곡이 없습니다.")).setEphemeral(true).queue()
            return
        }
        event.replyEmbeds(successEmbed("스킵", "⏭ **${skipped.info.title}** 을(를) 스킵했습니다.")).queue()
    }

    // 스킵 슬래시 명령의 이름을 생성하는 함수입니다.
    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "스킵")
}
