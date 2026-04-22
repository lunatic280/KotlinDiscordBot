package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.VoiceChannelManager
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class JoinCommand(
    private val voiceChannelManager: VoiceChannelManager,
) : SlashCommand {

    private val pendingGuilds = ConcurrentHashMap.newKeySet<Long>()

    override val name = "join"
    override val description = "봇을 음성 채널에 입장"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val targetChannel = event.member?.voiceState?.channel as? AudioChannel
        if (targetChannel == null) {
            event.replyEmbeds(errorEmbed("먼저 음성 채널에 입장해주세요.")).setEphemeral(true).queue()
            return
        }

        val audioManager = guild.audioManager
        val connectedChannel = audioManager.connectedChannel
        val connectionStatus = audioManager.connectionStatus

        if (connectionStatus.isConnecting() && connectedChannel?.idLong != targetChannel.idLong) {
            event.replyEmbeds(
                successEmbed("입장", "이미 음성 채널 연결을 처리 중입니다. 잠시 후 다시 시도해주세요.")
            ).setEphemeral(true).queue()
            return
        }

        if (!pendingGuilds.add(guild.idLong)) {
            event.replyEmbeds(
                errorEmbed("이미 입장 요청을 처리 중입니다. 잠시 후 다시 시도해주세요.")
            ).setEphemeral(true).queue()
            return
        }

        try {
            val result = voiceChannelManager.connect(guild, targetChannel)
            val message = when {
                result.alreadyConnected ->
                    "이미 **${result.targetChannelName}** 채널에 연결되어 있습니다."
                result.previousChannelName == null || result.previousChannelName == result.targetChannelName ->
                    "**${result.targetChannelName}** 채널에 입장했습니다."
                else ->
                    "**${result.previousChannelName}** 채널에서 **${result.targetChannelName}** 채널로 이동했습니다."
            }

            event.replyEmbeds(successEmbed("입장", message)).setEphemeral(result.alreadyConnected).queue()
        } catch (e: Exception) {
            event.replyEmbeds(
                errorEmbed("음성 채널 입장 중 오류가 발생했습니다: ${e.message ?: "알 수 없는 오류"}")
            ).setEphemeral(true).queue()
        } finally {
            pendingGuilds.remove(guild.idLong)
        }
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "입장")

    private fun ConnectionStatus.isConnecting(): Boolean =
        name.startsWith("CONNECTING_")
}
