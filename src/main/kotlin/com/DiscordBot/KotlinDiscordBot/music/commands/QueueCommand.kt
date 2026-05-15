package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.formatDuration
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import java.awt.Color

private const val PAGE_SIZE = 10

@Component
class QueueCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "queue"
    override val description = "대기열 목록"

    // 현재 재생 곡과 대기열 목록을 Discord 임베드로 보여주는 명령 처리 함수입니다.
    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val gmm = musicPlayerService.getOrCreate(guild)
        val current = gmm.player.playingTrack
        val queued = gmm.queue.snapshot()

        if (current == null && queued.isEmpty()) {
            event.replyEmbeds(errorEmbed("대기열이 비어있습니다.")).setEphemeral(true).queue()
            return
        }

        val embed = EmbedBuilder().setColor(Color.CYAN).setTitle("대기열")

        current?.let {
            val dur = if (it.info.isStream) "라이브" else formatDuration(it.duration)
            embed.addField("지금 재생 중", "[${it.info.title}](${it.info.uri}) `[$dur]`", false)
        }

        if (queued.isNotEmpty()) {
            val list = queued.take(PAGE_SIZE).mapIndexed { i, track ->
                val dur = if (track.info.isStream) "라이브" else formatDuration(track.duration)
                "`${i + 1}.` [${track.info.title}](${track.info.uri}) `[$dur]`"
            }.joinToString("\n")
            val remaining = if (queued.size > PAGE_SIZE) "\n... 외 ${queued.size - PAGE_SIZE}곡" else ""
            embed.addField("다음 곡 (${queued.size}곡)", list + remaining, false)
        }

        embed.setFooter("반복 모드: ${gmm.queue.loopMode.name}")
        event.replyEmbeds(embed.build()).queue()
    }

    // 대기열 조회 슬래시 명령의 이름을 생성하는 함수입니다.
    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "대기열")
}
