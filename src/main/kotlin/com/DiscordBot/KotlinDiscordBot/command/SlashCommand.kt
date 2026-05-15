package com.DiscordBot.KotlinDiscordBot.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

interface SlashCommand {
    val name: String
    val description: String
    // 슬래시 명령 이벤트를 실제로 처리하는 함수입니다.
    fun handle(event: SlashCommandInteractionEvent)
    // Discord에 등록할 슬래시 명령 메타데이터를 만드는 함수입니다.
    fun getCommandData(): SlashCommandData
}
