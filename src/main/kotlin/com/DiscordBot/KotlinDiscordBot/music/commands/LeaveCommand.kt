package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.VoiceChannelManager
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class LeaveCommand(private val voiceChannelManager: VoiceChannelManager) : SlashCommand {
    override val name = "leave"
    override val description = "봇 퇴장"

    // 봇이 연결된 음성 채널에서 나가도록 처리하는 명령 함수입니다.
    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        if (!guild.audioManager.isConnected) {
            event.replyEmbeds(errorEmbed("봇이 음성 채널에 없습니다.")).setEphemeral(true).queue()
            return
        }
        voiceChannelManager.disconnect(guild)
        event.replyEmbeds(successEmbed("퇴장", "음성 채널에서 나갔습니다.")).queue()
    }

    // 음성 채널 퇴장 슬래시 명령의 이름을 생성하는 함수입니다.
    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "퇴장")
}
