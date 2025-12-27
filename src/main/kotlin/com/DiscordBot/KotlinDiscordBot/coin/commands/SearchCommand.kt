package com.DiscordBot.KotlinDiscordBot.coin.commands

import com.DiscordBot.KotlinDiscordBot.coin.data.TickerDto
import com.DiscordBot.KotlinDiscordBot.coin.data.showRate
import com.DiscordBot.KotlinDiscordBot.coin.service.CoinService
import com.DiscordBot.KotlinDiscordBot.coin.util.Change
import com.DiscordBot.KotlinDiscordBot.coin.util.Market
import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

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
        val market = Market.Companion.fromKName(input)
        if (market == null) {
            event.reply("맞는 코인이 없어요. $market, $input").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()
        coinService.getCoin(market)
            .subscribe(
                { dto ->
                    val change = Change.fromApi(dto.change)
                    val pct = dto.showRate()

                    val eb = EmbedBuilder()
                        .setTitle("${market.code} 시세")              // KRW-BTC 시세
                        .setDescription(change.labelWithPct(pct))     // 🔴 ▲ 상승 (+1.23%)
                        .addField("시가", "%,d원".format(dto?.opening_price), true)
                        .addField("고가", "%,d원".format(dto?.high_price), true)
                        .addField("저가", "%,d원".format(dto?.low_price), true)
                        .addField("종가(최근 체결가)", "%,d원".format(dto?.trade_price), false)
                        .setTimestamp(Instant.now())

                    change.color?.let { eb.setColor(it) }            // 상승=빨강, 하락=파랑, 보합=검정

                    event.replyEmbeds(eb.build()).queue()
                },
                { err ->
                    event.hook.editOriginal("시세를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.").queue()
                }
            )

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