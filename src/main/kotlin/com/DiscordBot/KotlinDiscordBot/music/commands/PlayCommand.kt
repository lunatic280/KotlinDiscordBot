package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.LoadResult
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.service.VoiceChannelManager
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class PlayCommand(
    private val musicPlayerService: MusicPlayerService,
    private val voiceChannelManager: VoiceChannelManager,
) : SlashCommand {
    override val name = "play"
    override val description = "음악 재생"

    // URL 또는 검색어로 음악을 로드하고 음성 채널에서 재생하거나 대기열에 추가하는 명령 함수입니다.
    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val channel = event.member?.voiceState?.channel as? AudioChannel
        if (channel == null) {
            event.replyEmbeds(errorEmbed("먼저 음성 채널에 입장해주세요.")).setEphemeral(true).queue()
            return
        }
        val query = event.getOption("query")?.asString?.trim() ?: return

        event.deferReply().queue()
        voiceChannelManager.connect(guild, channel)

        musicPlayerService.load(guild, query).subscribe { result ->
            when (result) {
                is LoadResult.Single -> {
                    musicPlayerService.enqueue(guild, result.track)
                    event.hook.sendMessageEmbeds(
                        EmbedBuilder().setColor(Color.GREEN)
                            .setTitle("대기열 추가")
                            .setDescription("[${result.track.info.title}](${result.track.info.uri})")
                            .setFooter(event.user.effectiveName, event.user.avatarUrl)
                            .build()
                    ).queue()
                }
                is LoadResult.Playlist -> {
                    musicPlayerService.enqueueAll(guild, result.tracks)
                    event.hook.sendMessageEmbeds(
                        EmbedBuilder().setColor(Color.GREEN)
                            .setTitle("플레이리스트 추가")
                            .setDescription("**${result.name}** (${result.tracks.size}곡)")
                            .setFooter(event.user.effectiveName, event.user.avatarUrl)
                            .build()
                    ).queue()
                }
                is LoadResult.NotFound ->
                    event.hook.sendMessageEmbeds(errorEmbed("검색 결과가 없습니다.")).setEphemeral(true).queue()
                is LoadResult.Failed ->
                    event.hook.sendMessageEmbeds(errorEmbed("로드 실패: ${result.cause.message}")).setEphemeral(true).queue()
            }
        }
    }

    // 음악 재생 슬래시 명령의 검색어 옵션 정보를 생성하는 함수입니다.
    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "재생")
            .addOptions(
                OptionData(OptionType.STRING, "query", "URL 또는 검색어", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "검색어")
                    .setDescriptionLocalization(DiscordLocale.KOREAN, "YouTube URL 또는 검색어를 입력하세요")
            )
}
