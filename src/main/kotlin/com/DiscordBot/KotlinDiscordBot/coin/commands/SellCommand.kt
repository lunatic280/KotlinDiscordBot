package com.DiscordBot.KotlinDiscordBot.coin.commands

import com.DiscordBot.KotlinDiscordBot.coin.service.CoinService
import com.DiscordBot.KotlinDiscordBot.coin.util.Market
import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import java.time.Duration
import java.util.concurrent.TimeoutException
import com.DiscordBot.KotlinDiscordBot.member.service.MemberService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class SellCommand(
    private val memberService: MemberService,
    private val coinService: CoinService
): SlashCommand {
    override val name: String = "sell"
    override val description: String = "sellcoin"

    override fun handle(event: SlashCommandInteractionEvent) {
        val inputCoinName = event.getOption("name")?.asString?.trim()
        val inputCoinCount = event.getOption("count")?.asString?.trim()

        if (inputCoinName.isNullOrBlank() || inputCoinCount.isNullOrBlank()) {
            event.reply("코인 이름 또는 코인 개수가 비었어요. 예) 비트코인 / 이더리움 / 도지코인 / 1 2 3")
                .setEphemeral(true).queue()
            return
        }

        val count = inputCoinCount.toLongOrNull()
        if (count == null || count <= 0) {
            event.reply("코인 개수는 1 이상의 숫자여야 합니다.")
                .setEphemeral(true).queue()
            return
        }

        val market = Market.Companion.fromKName(inputCoinName)
        if (market == null) {
            event.reply("맞는 코인이 없어요: $inputCoinName").setEphemeral(true).queue()
            return
        }

        val userId = event.user.idLong.toString()
        if (!memberService.existsMember(userId)) {
            event.reply("먼저 등록을 해야합니다").setEphemeral(true).queue()
            return
        }
        val member = memberService.getMember(userId)

        event.deferReply().queue()
        coinService.getCoin(market)
            .timeout(Duration.ofSeconds(10))
            .subscribe(
                { dto ->
                    try {
                        val result = coinService.sellCoin(
                            dto = member,
                            market = market,
                            count = count,
                            cost = dto.opening_price
                        )
                        event.hook.sendMessage(
                            "${market.koreanName} ${count}개 판매 완료! (개당 ${dto.opening_price}원)"
                        ).queue()
                    } catch (e: IllegalArgumentException) {
                        event.hook.sendMessage("판매 실패: ${e.message}")
                            .setEphemeral(true).queue()
                    }
                },
                { error ->
                    val message = if (error is TimeoutException) {
                        "요청 시간이 초과되었습니다. 잠시 후 다시 시도해주세요."
                    } else {
                        "코인 시세 조회 실패. 잠시 후 다시 시도해주세요."
                    }
                    event.hook.sendMessage(message).setEphemeral(true).queue()
                }
            )
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "코인판매")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "코인 판매하기")
            .addOptions(
                OptionData(OptionType.STRING, "name", "select coin", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "코인이름")
                    .setDescriptionLocalization(DiscordLocale.KOREAN, "코인이름을 적으세요")
            )
            .addOptions(
                OptionData(OptionType.STRING, "count", "coin count", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "코인개수")
                    .setDescriptionLocalization(DiscordLocale.KOREAN, "판매할 코인개수를 입력하세요")
            )
    }
}
