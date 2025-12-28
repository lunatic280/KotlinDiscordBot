package com.DiscordBot.KotlinDiscordBot.coin.commands

import com.DiscordBot.KotlinDiscordBot.coin.service.CoinService
import com.DiscordBot.KotlinDiscordBot.coin.util.Market
import java.time.Duration
import java.util.concurrent.TimeoutException
import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.member.service.MemberService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import java.awt.Color
import java.text.NumberFormat
import java.util.Locale

@Component
class BuyCommand(
    private val memberService: MemberService,
    private val coinService: CoinService
): SlashCommand {
    override val name: String = "buy"
    override val description: String = "buy coin"

    override fun handle(event: SlashCommandInteractionEvent) {
        val inputCoinName = event.getOption("name")?.asString?.trim()
        val inputCoinCount = event.getOption("count")?.asString?.trim()

        if (inputCoinName.isNullOrBlank() || inputCoinCount.isNullOrBlank()) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("입력 오류")
                    .setDescription("코인 이름 또는 개수가 비었어요.")
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val count = inputCoinCount.toLongOrNull()
        if (count == null || count <= 0) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("입력 오류")
                    .setDescription("코인 개수는 1 이상의 숫자여야 합니다.")
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val market = Market.Companion.fromKName(inputCoinName)
        if (market == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("코인 없음")
                    .setDescription("'$inputCoinName' 코인을 찾을 수 없습니다.")
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val userId = event.user.idLong.toString()
        if (!memberService.existsMember(userId)) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setColor(Color.ORANGE)
                    .setTitle("등록 필요")
                    .setDescription("먼저 등록을 해야합니다.")
                    .build()
            ).setEphemeral(true).queue()
            return
        }
        val member = memberService.getMember(userId)
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)

        event.deferReply().queue()
        coinService.getCoin(market)
            .timeout(Duration.ofSeconds(10))
            .subscribe(
                { dto ->
                    try {
                        val result = coinService.buyCoin(
                            dto = member,
                            market = market,
                            count = count,
                            cost = dto.opening_price //현재가 사용
                        )
                        val totalCost = dto.opening_price * count

                        val embed = EmbedBuilder()
                            .setColor(Color.GREEN)
                            .setTitle("구매 완료")
                            .setDescription("${market.koreanName} 구매에 성공했습니다.")
                            .addField("코인", market.koreanName, true)
                            .addField("개수", "${formatter.format(count)}개", true)
                            .addField("개당 가격", "${formatter.format(dto.opening_price)}원", true)
                            .addField("총 금액","${formatter.format(totalCost)}원", false)
                            .setFooter("${event.user.effectiveName}", event.user.avatarUrl)
                            .build()

                        event.hook.sendMessageEmbeds(embed).queue()
                    } catch (e: IllegalArgumentException) {
                        val errorEmbed = EmbedBuilder()
                            .setColor(Color.RED)
                            .setTitle("구매 실패")
                            .setDescription(e.message ?: "알 수 없는 오류")
                            .build()

                        event.hook.sendMessageEmbeds(errorEmbed).setEphemeral(true).queue()
                    }
                },
                { error ->
                    val message = if (error is TimeoutException) {
                        "요청 시간이 초과되었습니다."
                    } else {
                        "코인 시세 조회 실패. 잠시 후 다시 시도해주세요."
                    }

                    val errorEmbed = EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle("오류 발생")
                        .setDescription(message)
                        .build()

                    event.hook.sendMessageEmbeds(errorEmbed).setEphemeral(true).queue()
                }
            )
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "코인구매")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "코인 구매하기")
            .addOptions(
                OptionData(OptionType.STRING, "name", "select coin", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "코인이름")
                    .setDescriptionLocalization(DiscordLocale.KOREAN, "코인이름을 적으세요")

            )
            .addOptions(
                OptionData(OptionType.STRING, "count", "coin count", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "코인개수")
                    .setDescriptionLocalization(DiscordLocale.KOREAN,"구매할 코인개수를 입력하세요")
            )
    }

}