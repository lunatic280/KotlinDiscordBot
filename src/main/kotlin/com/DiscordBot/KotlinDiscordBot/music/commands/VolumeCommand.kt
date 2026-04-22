package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class VolumeCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "volume"
    override val description = "볼륨 설정 (0~150)"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val level = event.getOption("level")?.asInt
        if (level == null || level !in 0..150) {
            event.replyEmbeds(errorEmbed("볼륨은 0~150 사이의 숫자를 입력해주세요.")).setEphemeral(true).queue()
            return
        }
        musicPlayerService.setVolume(guild, level)
        event.replyEmbeds(successEmbed("볼륨 변경", "🔊 볼륨을 **$level**으로 설정했습니다.")).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "볼륨")
            .addOptions(
                OptionData(OptionType.INTEGER, "level", "0~150", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "볼륨크기")
                    .setMinValue(0)
                    .setMaxValue(150)
            )
}
