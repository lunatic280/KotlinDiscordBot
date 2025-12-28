package com.DiscordBot.KotlinDiscordBot.money.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.money.service.WalletService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
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
            event.reply("아직 등록된 사용자가 없습니다").setEphemeral(true).queue()
            return
        }

        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val guild = event.guild

        val rankingText = rankings.mapIndexed { index, wallet ->
            val rank = index + 1
            val medal = when (rank) {
                1 -> "1."
                2 -> "2."
                3 -> "3."
                else -> "$rank."
            }
            val memberName = guild?.getMemberById(wallet.memberId)?.effectiveName
                ?: "Unknown"
            val totalFormatted = formatter.format(wallet.totalWealth)
            "$medal **$memberName** - ${totalFormatted}원"
        }.joinToString("\n")

        val message = """                                                                                                                        
              **재산 랭킹 TOP 10**                                                                                                                 
              ━━━━━━━━━━━━━━━━━                                                                                                                    
              $rankingText                                                                                                                         
          """.trimIndent()

        event.reply(message).queue()
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "랭킹")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "재산 랭킹 확인하기")
    }
}