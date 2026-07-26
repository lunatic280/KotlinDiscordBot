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
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeoutException

@Component
class AskGeminiCommand(
    private val geminiService: GemmaService
) : SlashCommand {
    private val log = LoggerFactory.getLogger(AskGeminiCommand::class.java)

    override val name: String = "askgemini"
    override val description: String = "AskGemini"

    override fun handle(event: SlashCommandInteractionEvent) {
        val commandStartedAt = System.nanoTime()
        val requestId = UUID.randomUUID().toString().take(8)

        val prompt = event.getOption("prompt")?.asString?.trim()
        val inputReadMs = elapsedMs(commandStartedAt)

        if (prompt.isNullOrBlank()) {
            event.reply("질문 내용이 비었습니다.").setEphemeral(true).queue()
            return
        }

        val deferStartedAt = System.nanoTime()
        event.deferReply().queue(
            {
                val discordDeferMs = elapsedMs(deferStartedAt)
                val serviceObservedStartedAt = System.nanoTime()

                geminiService.generateText(prompt, requestId)
                    .timeout(Duration.ofSeconds(30))
                    .subscribe(
                        { result ->
                            val serviceObservedMs = elapsedMs(serviceObservedStartedAt)
                            val embedBuildStartedAt = System.nanoTime()
                            val modelCallSummary = result.timing.modelCalls.joinToString(" + ") {
                                "${it.model} total=${formatMs(it.elapsedMs)}ms/" +
                                    "http=${formatMs(it.httpAndBodyMs)}ms/" +
                                    "json=${formatMs(it.responseJsonParseMs)}ms"
                            }

                            val embed = EmbedBuilder()
                                .setColor(Color(0x4285F4))
                                .setTitle("Gemini 답변")
                                .setDescription(result.answer.take(4000))
                                .setFooter(
                                    "AI 처리: ${formatMs(result.timing.serviceTotalMs)}ms",
                                )
                                .build()
                            val embedBuildMs = elapsedMs(embedBuildStartedAt)
                            val discordSendStartedAt = System.nanoTime()

                            event.hook.sendMessageEmbeds(embed).queue(
                                {
                                    val discordSendMs = elapsedMs(discordSendStartedAt)
                                    val endToEndMs = elapsedMs(commandStartedAt)

                                    log.info(
                                        "[{}] timing input={}ms, discordDefer={}ms, " +
                                            "requestBuild={}ms, requestJsonSerialize={}ms, " +
                                            "modelCalls=[{}], " +
                                            "responseExtract={}ms, service={}ms, " +
                                            "serviceObserved={}ms, embedBuild={}ms, " +
                                            "discordSend={}ms, endToEnd={}ms",
                                        requestId,
                                        formatMs(inputReadMs),
                                        formatMs(discordDeferMs),
                                        formatMs(result.timing.requestBuildMs),
                                        formatMs(result.timing.requestJsonSerializeMs),
                                        modelCallSummary,
                                        formatMs(result.timing.responseExtractMs),
                                        formatMs(result.timing.serviceTotalMs),
                                        formatMs(serviceObservedMs),
                                        formatMs(embedBuildMs),
                                        formatMs(discordSendMs),
                                        formatMs(endToEndMs),
                                    )
                                },
                                { sendError ->
                                    log.error(
                                        "[{}] Discord response send failed after {}ms",
                                        requestId,
                                        formatMs(elapsedMs(discordSendStartedAt)),
                                        sendError,
                                    )
                                },
                            )
                        },
                        { error ->
                            val serviceObservedMs = elapsedMs(serviceObservedStartedAt)
                            val endToEndMs = elapsedMs(commandStartedAt)

                            log.error(
                                "[{}] Gemini request failed. input={}ms, discordDefer={}ms, " +
                                    "serviceObserved={}ms, endToEnd={}ms",
                                requestId,
                                formatMs(inputReadMs),
                                formatMs(discordDeferMs),
                                formatMs(serviceObservedMs),
                                formatMs(endToEndMs),
                                error,
                            )

                            val message = when (error) {
                                is TimeoutException -> "Gemini 응답 시간이 초과되었습니다."
                                else -> "Gemini 호출에 실패했습니다: ${error.message ?: "unknown error"}"
                            }
                            event.hook.sendMessage(message).setEphemeral(true).queue()
                        },
                    )
                },
            { error ->
                log.error(
                    "[{}] Discord defer failed after {}ms",
                    requestId,
                    formatMs(elapsedMs(deferStartedAt)),
                    error,
                )
            },
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

    private fun elapsedMs(startedAt: Long): Double =
        (System.nanoTime() - startedAt) / 1_000_000.0

    private fun formatMs(value: Double): String =
        String.format(Locale.US, "%.3f", value)
}
