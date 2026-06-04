package com.DiscordBot.KotlinDiscordBot.notification.command

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class RegistrationUrlCommand : SlashCommand {
    override val name: String = "registrationUrl"
    override val description: String = "registration url"

    override fun handle(event: SlashCommandInteractionEvent) {
        val input = event.getOption("URL")?.asString?.trim()
        if (input.isNullOrBlank()) {
            event.reply("URL 내용이 없습니다.").queue()
            return
        }
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "URL등록")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "보고싶은 페이지를 등록하세요.")
            .addOptions(
                OptionData(OptionType.STRING, "URL", "page", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "URL 주소")
                    .setDescriptionLocalization(DiscordLocale.KOREAN, "URL주소를 입력하세요")
            )
    }
}