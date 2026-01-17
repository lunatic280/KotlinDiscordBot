package com.DiscordBot.KotlinDiscordBot.money.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.member.service.MemberService
import com.DiscordBot.KotlinDiscordBot.money.repository.PositionRepository
import com.DiscordBot.KotlinDiscordBot.money.service.PositionService
import com.DiscordBot.KotlinDiscordBot.money.service.WalletService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import java.awt.Color
import java.text.NumberFormat
import java.util.Locale

@Component
class MyWealthCommand(
    private val memberService: MemberService,
    private val walletService: WalletService,
    private val positionService: PositionService
) : SlashCommand {
    override val name: String = "wallet"
    override val description: String = "check my wallet"

    override fun handle(event: SlashCommandInteractionEvent) {
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

        val wallet = walletService.getWalletByUserId(userId)
        if (wallet == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("오류")
                    .setDescription("지갑 정보를 찾을 수 없습니다.")
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val listPosition = positionService.getPositionMarketList(wallet.id!!)

        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val coinValue = wallet.totalWealth - wallet.cash

        val embed = EmbedBuilder()
            .setColor(Color(0x5865F2))
            .setTitle("${event.user.effectiveName}님의 재산")
            .setThumbnail(event.user.avatarUrl)
            .addField("현금", "${formatter.format(wallet.cash)}원", true)
            .addField("코인 가치", "${formatter.format(coinValue)}원", true)
            .addField("\u200B", "\u200B", true)
            .addField("총 재산", "${formatter.format(wallet.totalWealth)}원", false)

        if (listPosition.isEmpty()) {
            embed.addField("보유 코인", "보유한 코인이 없습니다.", false)
        } else {
            listPosition.forEach { position ->
                embed.addField(
                    position.market.koreanName,
                    "${formatter.format(position.marketCount)}개",
                    true
                )
            }
        }

        embed.setFooter("1분마다 코인 가치가 업데이트됩니다")

        event.replyEmbeds(embed.build()).queue()
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "내재산")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "내 재산 확인하기")
    }
}
