package com.DiscordBot.KotlinDiscordBot.breakfast.commands

import com.DiscordBot.KotlinDiscordBot.breakfast.domain.BreakfastInfo
import com.DiscordBot.KotlinDiscordBot.breakfast.domain.BreakfastUniversity
import com.DiscordBot.KotlinDiscordBot.breakfast.service.BreakfastQueryService
import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CompletableFuture

@Component
class BreakfastCommand(
    private val breakfastQueryService: BreakfastQueryService
) : SlashCommand {

    private val log = LoggerFactory.getLogger(BreakfastCommand::class.java)

    override val name: String = "breakfast"
    override val description: String = "lookup KRW 1,000 breakfast menus"

    // 조식 조회 명령 옵션을 검증하고 대상 날짜의 조식 정보를 비동기로 응답하는 함수입니다.
    override fun handle(event: SlashCommandInteractionEvent) {
        val universityOption = event.getOption("university")?.asString
        if (universityOption.isNullOrBlank()) {
            event.reply("Please choose a university.").setEphemeral(true).queue()
            return
        }

        val university = BreakfastUniversity.fromOption(universityOption)
        if (university == null) {
            event.reply("Unsupported university: $universityOption").setEphemeral(true).queue()
            return
        }

        val targetDate = try {
            event.getOption("date")?.asString?.trim()?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
                ?: LocalDate.now(SEOUL_ZONE_ID)
        } catch (_: Exception) {
            event.reply("Date must use yyyy-MM-dd.").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()
        CompletableFuture.supplyAsync {
            breakfastQueryService.fetch(university, targetDate)
        }.whenComplete { info, error ->
            if (error != null) {
                log.warn("breakfast command failed. university={}, date={}", university, targetDate, error)
                event.hook.editOriginal("Failed to load breakfast info: ${error.cause?.message ?: error.message}").queue()
                return@whenComplete
            }

            event.hook.editOriginal(formatReply(info)).queue()
        }
    }

    // 조식 조회 슬래시 명령의 대학과 날짜 옵션 정보를 생성하는 함수입니다.
    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .addOptions(
                OptionData(OptionType.STRING, "university", "target university", true)
                    .addChoice("가톨릭대학교", BreakfastUniversity.CATHOLIC.optionValue)
                    .addChoice("충남대학교", BreakfastUniversity.CNU.optionValue),
                OptionData(OptionType.STRING, "date", "yyyy-MM-dd, defaults to today", false)
            )
    }

    // 조회된 조식 정보를 Discord 메시지 본문 문자열로 포맷하는 함수입니다.
    private fun formatReply(info: BreakfastInfo): String {
        val menu = info.menuItems.joinToString("\n") { "- $it" }
        val sources = info.sourceUrls.joinToString("\n") { "- $it" }
        val noteLine = info.note?.takeIf { it.isNotBlank() }?.let { "\nNote: $it" }.orEmpty()

        return buildString {
            append(info.university.displayName)
            append('\n')
            append("Date: ${info.date}")
            append('\n')
            append("Time: ${info.time}")
            append('\n')
            append("Location: ${info.location}")
            append('\n')
            append("Price: ${info.price}")
            append('\n')
            append("Menu:\n$menu")
            append(noteLine)
            append("\nSources:\n$sources")
        }
    }

    companion object {
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
