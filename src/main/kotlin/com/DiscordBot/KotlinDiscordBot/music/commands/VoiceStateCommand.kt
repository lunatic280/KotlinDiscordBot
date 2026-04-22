package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class VoiceStateCommand : SlashCommand {
    override val name: String = "voice-state"

    override val description: String = "check voice state"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild
        val member = event.member

        if (guild == null || member == null) {
            event.reply("길드 또는 멤버 정보를 확인할 수 없습니다.").setEphemeral(true).queue()
            return
        }

        val userChannel = member.voiceState?.channel as? AudioChannel
        val botChannel = guild.audioManager.connectedChannel

        val userMessage = if (userChannel != null) {
            "사용자 채널: ${userChannel.name}"
        } else {
            "사용자는 현재 음성 채널에 없습니다."
        }

        val botMessage = if (botChannel != null) {
            "봇 채널: ${botChannel.name}"
        } else {
            "봇은 현재 음성 채널에 연결되어 있지 않습니다"
        }

        event.reply("$userMessage\n$botMessage")
            .setEphemeral(true).queue()
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "상태확인")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "보이스 상태 확인")
    }
}