package com.DiscordBot.KotlinDiscordBot.money.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
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
class RankingCommand(
    private val walletService: WalletService
) : SlashCommand {
    override val name: String = "ranking"
    override val description: String = "Wealth ranking"

    override fun handle(event: SlashCommandInteractionEvent) {
        val rankings = walletService.getTop10Ranking()

        if (rankings.isEmpty()) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setColor(Color.ORANGE)
                    .setTitle("랭킹 없음")
                    .setDescription("아직 등록된 사용자가 없습니다.")
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val guild = event.guild

        val rankingText = rankings.mapIndexed { index, wallet ->
            val rank = index + 1
            val medal = when (rank) {
                1 -> ":first_place:"
                2 -> ":second_place:"
                3 -> ":third_place:"
                else -> "**$rank.**"
            }
            val memberName = guild?.getMemberById(wallet.memberId)?.effectiveName ?: "Unknown"
            val totalFormatted = formatter.format(wallet.totalWealth)
            "$medal $memberName - ${totalFormatted}원"
        }.joinToString("\n")

        val embed = EmbedBuilder()
            .setColor(Color(0xFFD700))
            .setTitle(":trophy: 재산 랭킹 TOP 10")
            .setDescription(rankingText)
            .setFooter("1분마다 업데이트됩니다")
            .build()

        event.replyEmbeds(embed).queue()
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "랭킹")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "재산 랭킹 확인하기")
    }
}
