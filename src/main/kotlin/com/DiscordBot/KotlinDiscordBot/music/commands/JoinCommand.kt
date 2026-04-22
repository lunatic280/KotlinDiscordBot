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
    override val description: String = "봇을 음성 채널에 입장"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val member = event.member ?: return
        val selfMember = guild.selfMember

        val memberVoiceState = member.voiceState
        val targetChannel = memberVoiceState?.channel as? AudioChannel

        // 1) 명령 사용자가 음성 채널에 있는지 확인
        if (targetChannel == null) {
            event.replyEmbeds(
                errorEmbed("먼저 음성 채널에 입장해주세요.")
            ).setEphemeral(true).queue()
            return
        }

        // 2) 봇 권한 확인
        if (!selfMember.hasPermission(targetChannel, Permission.VOICE_CONNECT)) {
            event.replyEmbeds(
                errorEmbed("봇에게 해당 음성 채널의 연결 권한(VOICE_CONNECT)이 없습니다.")
            ).setEphemeral(true).queue()
            return
        }

        if (!selfMember.hasPermission(targetChannel, Permission.VOICE_SPEAK)) {
            event.replyEmbeds(
                errorEmbed("봇에게 해당 음성 채널의 말하기 권한(VOICE_SPEAK)이 없습니다.")
            ).setEphemeral(true).queue()
            return
        }

        val audioManager = guild.audioManager
        val connectedChannel = audioManager.connectedChannel

        // 3) 이미 같은 채널에 있으면 재연결하지 않음
        if (connectedChannel?.idLong == targetChannel.idLong && audioManager.isConnected) {
            event.replyEmbeds(
                successEmbed("입장", "이미 **${targetChannel.name}** 채널에 연결되어 있습니다.")
            ).setEphemeral(true).queue()
            return
        }

        // 4) 이미 다른 채널에 있으면 join에서는 이동하지 않음
        //    필요하면 별도의 /move 명령으로 분리하는 편이 안전함
        if (connectedChannel != null && audioManager.isConnected) {
            event.replyEmbeds(
                errorEmbed(
                    "봇이 이미 **${connectedChannel.name}** 채널에 연결되어 있습니다.\n" +
                            "먼저 퇴장시키거나, 별도의 이동 명령을 사용해주세요."
                )
            ).setEphemeral(true).queue()
            return
        }

        // 5) 연결 중이면 중복 요청 차단
        if (audioManager.connectionStatus.name.startsWith("CONNECTING_")) {
            event.replyEmbeds(
                errorEmbed("현재 음성 채널 연결을 처리 중입니다. 잠시 후 다시 시도해주세요.")
            ).setEphemeral(true).queue()
            return
        }

        try {
            val result = voiceChannelManager.connect(guild, targetChannel)

            event.replyEmbeds(
                successEmbed(
                    "입장",
                    when {
                        result.alreadyConnected ->
                            "이미 **${result.targetChannelName}** 채널에 연결되어 있습니다."

                        result.previousChannelName == null ->
                            "**${result.targetChannelName}** 채널에 입장했습니다."

                        else ->
                            "**${result.previousChannelName}** 채널에서 **${result.targetChannelName}** 채널로 이동했습니다."
                    }
                )
            ).queue()
        } catch (e: Exception) {
            event.replyEmbeds(
                errorEmbed("음성 채널 입장 중 오류가 발생했습니다: ${e.message ?: "알 수 없는 오류"}")
            ).setEphemeral(true).queue()
        }
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "입장")
}