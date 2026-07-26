package com.DiscordBot.KotlinDiscordBot.pubg.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.pubg.service.PubgService
import com.DiscordBot.KotlinDiscordBot.pubg.utils.PubgUtils
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.dv8tion.jda.api.EmbedBuilder
import org.slf4j.LoggerFactory
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class PubgAnotherPlayerResearch(
    private val pubgService: PubgService,
    private val pubgUtils: PubgUtils
): SlashCommand {
    private val log = LoggerFactory.getLogger(PubgAnotherPlayerResearch::class.java)
    private val objectMapper = jacksonObjectMapper()
    override val name: String = "battlegroundanotherplayersearch"
    override val description: String = "research another player match"

    override fun handle(event: SlashCommandInteractionEvent) {
        val inputAnotherPlayer = event.getOption("player")?.asString?.trim()

        if (inputAnotherPlayer.isNullOrBlank()) {
            log.warn("Invalid input-another player.")
            event.reply("검색하고자하는 플레이어를 입력하세요.").setEphemeral(true).queue()
            return
        }

        val playerResponse = pubgService.getPlayersByName(inputAnotherPlayer)
        log.info("PlayerResponse $playerResponse")
        val playerRoot = objectMapper.readTree(playerResponse)
        log.info("PlayerRoot $playerRoot")
        val playerName = playerRoot["data"][0]["attributes"]["name"].asText()
        log.info("playerName: $playerName")
        val result = pubgService.getMyLatestTeamSummary(playerName)

        if (result == null) {
            event.reply("최근에 플레이한 전적이 없습니다.")
                .setEphemeral(true)
                .queue()
            return
        }
        val embed = pubgUtils.makeGraphEmbed(result)

        event.replyEmbeds(embed.build())
            .queue()
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "다른사용자검색")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "다른 사용자의 아이디를 입력하세요.")
            .addOptions(
                OptionData(
                    OptionType.STRING,
                    "player",
                    "PUBG player name",
                    true
                )
                    .setNameLocalization(
                        DiscordLocale.KOREAN,
                        "플레이어이름"
                    )
                    .setDescriptionLocalization(
                        DiscordLocale.KOREAN,
                        "플레이어 이름을 입력하세요."
                    )
            )
    }
}