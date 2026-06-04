package com.DiscordBot.KotlinDiscordBot.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class PingCommand: SlashCommand {
    override val name = "ping"
    override val description = "퐁!"
    // ping 명령에 Pong 응답을 보내는 함수입니다.
    override fun handle(event: SlashCommandInteractionEvent) {
        event.reply("Pong!").queue()
    }

    // ping 슬래시 명령 등록 정보를 생성하는 함수입니다.
    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
    }
}
