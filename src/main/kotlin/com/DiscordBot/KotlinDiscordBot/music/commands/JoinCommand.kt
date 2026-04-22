package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.VoiceChannelManager
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class JoinCommand(
    private val voiceChannelManager: VoiceChannelManager,
) : SlashCommand {

    override val name: String = "join"
    override val description: String = "join voice channel"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val member = event.member ?: return
        val selfMember = guild.selfMember

        val targetChannel = member.voiceState?.channel as? AudioChannel
        if (targetChannel == null) {
            event.replyEmbeds(errorEmbed("먼저 음성 채널에 입장해주세요."))
                .setEphemeral(true)
                .queue()
            return
        }

        // join에 필요한 최소 권한: VOICE_CONNECT
        if (!selfMember.hasPermission(targetChannel, Permission.VOICE_CONNECT)) {
            event.replyEmbeds(errorEmbed("봇에게 해당 음성 채널의 연결 권한이 없습니다."))
                .setEphemeral(true)
                .queue()
            return
        }

        val audioManager = guild.audioManager
        val botChannel = audioManager.connectedChannel

        // 이미 같은 채널
        if (botChannel?.idLong == targetChannel.idLong && audioManager.isConnected) {
            event.replyEmbeds(
                successEmbed("입장", "이미 **${targetChannel.name}** 채널에 연결되어 있습니다.")
            ).setEphemeral(true).queue()
            return
        }

        // 이미 다른 채널
        if (botChannel != null && audioManager.isConnected) {
            event.replyEmbeds(
                errorEmbed(
                    "봇이 이미 **${botChannel.name}** 채널에 연결되어 있습니다.\n" +
                            "먼저 /leave 하거나, 별도 /move 명령을 사용해주세요."
                )
            ).setEphemeral(true).queue()
            return
        }

        // openAudioConnection 은 비동기이므로 3초 안에 응답하도록 defer 선행
        event.deferReply().queue()
        try {
            voiceChannelManager.connect(guild, targetChannel)
            event.hook.sendMessageEmbeds(
                successEmbed("입장", "**${targetChannel.name}** 채널에 입장했습니다.")
            ).queue()
        } catch (e: Exception) {
            event.hook.sendMessageEmbeds(
                errorEmbed("음성 채널 입장 중 오류가 발생했습니다: ${e.message ?: "알 수 없는 오류"}")
            ).setEphemeral(true).queue()
        }
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "입장")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "채널에 입장합니다.")
}