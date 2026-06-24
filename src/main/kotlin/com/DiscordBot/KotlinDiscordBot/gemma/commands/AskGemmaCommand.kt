package com.DiscordBot.KotlinDiscordBot.gemini.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.gemma.service.GemmaService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Duration
import java.util.concurrent.TimeoutException

@Component
class AskGeminiCommand(
    private val geminiService: GemmaService
) : SlashCommand {
    private val log = LoggerFactory.getLogger(AskGeminiCommand::class.java)

    override val name: String = "askgemini"
    override val description: String = "AskGemini"

    override fun handle(event: SlashCommandInteractionEvent) {
        val prompt = event.getOption("prompt")?.asString?.trim()

        if (prompt.isNullOrBlank()) {
            event.reply("질문 내용이 비었습니다.").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()

        geminiService.generateText(prompt)
            .timeout(Duration.ofSeconds(30))
            .subscribe(
                { answer ->
                    val embed = EmbedBuilder()
                        .setColor(Color(0x4285F4))
                        .setTitle("Gemini 답변")
                        .setDescription(answer.take(4000))
                        .build()

                    event.hook.sendMessageEmbeds(embed).queue()
                },
                { error ->
                    log.error("Failed to handle Gemini command", error)
                    val message = when (error) {
                        is TimeoutException -> "Gemini 응답 시간이 초과되었습니다."
                        else -> "Gemini 호출에 실패했습니다: ${error.message ?: "unknown error"}"
                    }
                    event.hook.sendMessage(message).setEphemeral(true).queue()
                }
            )
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "제미나이질문")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "Gemini에게 질문합니다.")
            .addOptions(
                OptionData(OptionType.STRING, "prompt", "question", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "질문")
                    .setDescriptionLocalization(DiscordLocale.KOREAN, "Gemini에게 물어볼 내용을 입력하세요.")
            )
    }
}
