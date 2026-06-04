package com.DiscordBot.KotlinDiscordBot

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class SlashCommandListener(private val commands: List<SlashCommand>) : ListenerAdapter() {

    private val commandMap = commands.associateBy { it.name }

    // 들어온 슬래시 명령 이름에 맞는 명령 객체를 찾아 실행하는 이벤트 처리 함수입니다.
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val command = commandMap[event.name]
        if (command != null) {
            command.handle(event)
        } else {
            event.reply("Unknown command").setEphemeral(true).queue()
        }
    }
}
