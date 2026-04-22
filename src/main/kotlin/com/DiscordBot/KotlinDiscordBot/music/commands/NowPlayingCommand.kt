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

@Component
class NowPlayingCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "nowplaying"
    override val description = "현재 재생 중인 곡"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val track = musicPlayerService.current(guild)
        if (track == null) {
            event.replyEmbeds(errorEmbed("현재 재생 중인 곡이 없습니다.")).setEphemeral(true).queue()
            return
        }
        val position = formatDuration(track.position)
        val duration = if (track.info.isStream) "라이브" else formatDuration(track.duration)
        event.replyEmbeds(
            EmbedBuilder()
                .setColor(Color.CYAN)
                .setTitle("지금 재생 중")
                .setDescription("[${track.info.title}](${track.info.uri})")
                .addField("진행", "`$position / $duration`", true)
                .addField("채널", track.info.author, true)
                .build()
        ).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "지금곡")
}
