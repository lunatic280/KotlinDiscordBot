package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class StopCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "stop"
    override val description = "재생 정지 및 대기열 초기화"

    // 현재 재생을 멈추고 대기열을 비우는 명령 처리 함수입니다.
    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        musicPlayerService.stop(guild)
        event.replyEmbeds(successEmbed("정지", "⏹ 재생을 멈추고 대기열을 비웠습니다.")).queue()
    }

    // 정지 슬래시 명령의 이름을 생성하는 함수입니다.
    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "정지")
}
