package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.VoiceChannelManager
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class JoinCommand(private val voiceChannelManager: VoiceChannelManager) : SlashCommand {
    override val name = "join"
    override val description = "봇을 음성 채널에 입장"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val channel = event.member?.voiceState?.channel as? AudioChannel
        if (channel == null) {
            event.replyEmbeds(errorEmbed("먼저 음성 채널에 입장해주세요.")).setEphemeral(true).queue()
            return
        }
        voiceChannelManager.connect(guild, channel)
        event.replyEmbeds(successEmbed("입장", "**${channel.name}** 채널에 입장했습니다.")).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "입장")
}
