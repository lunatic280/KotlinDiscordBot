package com.DiscordBot.KotlinDiscordBot.coin

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import okio.Options
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SearchCommand(
    private val coinService: CoinService,
) : SlashCommand {
    private val log = LoggerFactory.getLogger(SearchCommand::class.java)
    override val name = "searchcoin"
    override val description = "search coin"
    override fun handle(event: SlashCommandInteractionEvent) {
        val input = event.getOption("name")?.asString?.trim()
        if (input.isNullOrBlank()) {
            event.reply("코인 이름이 비었어요. 예) 비트코인 / 이더리움 / 도지코인")
                .setEphemeral(true).queue()
            return
        }
        val market = Market.fromKName(input)
        if (market == null) {
            event.reply("맞는 코인이 없어요. $market, $input").setEphemeral(true).queue()
            return
        }
        val result = coinService.getCoin(market)
        event.reply("$input, $market, $result, $log.").queue()
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "코인검색")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "코인 시세를 검색해보아요.")
            .addOptions(
                OptionData(OptionType.STRING, "name", "coin", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "코인이름")
                    .setDescriptionLocalization(DiscordLocale.KOREAN, "코인이름을 한글로 입력하세요")
            )
    }
}