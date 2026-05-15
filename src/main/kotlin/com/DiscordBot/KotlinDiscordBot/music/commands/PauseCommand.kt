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
class PauseCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "pause"
    override val description = "일시정지"

    // 현재 재생 중인 음악을 일시정지하는 명령 처리 함수입니다.
    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        if (musicPlayerService.current(guild) == null) {
            event.replyEmbeds(errorEmbed("현재 재생 중인 곡이 없습니다.")).setEphemeral(true).queue()
            return
        }
        musicPlayerService.setPaused(guild, true)
        event.replyEmbeds(successEmbed("일시정지", "⏸ 재생을 일시정지했습니다.")).queue()
    }

    // 일시정지 슬래시 명령의 이름을 생성하는 함수입니다.
    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "일시정지")
}
