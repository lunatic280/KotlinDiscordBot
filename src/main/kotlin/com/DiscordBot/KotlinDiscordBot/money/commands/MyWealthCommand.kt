package com.DiscordBot.KotlinDiscordBot.money.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.member.service.MemberService
import com.DiscordBot.KotlinDiscordBot.money.service.WalletService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import java.text.NumberFormat
import java.util.Locale

@Component
class MyWealthCommand(
    private val memberService: MemberService,
    private val walletService: WalletService
) : SlashCommand{
    override val name: String = "wallet"
    override val description: String = "checkmywallet"

    override fun handle(event: SlashCommandInteractionEvent) {
        val userId = event.user.idLong.toString()

        if (!memberService.existsMember(userId)) {
            event.reply("먼저 등록을 해야합니다").setEphemeral(true).queue()
            return
        }

        val wallet = walletService.getWalletByUserId(userId)
        if (wallet == null) {
            event.reply("지갑 정보를 찾을 수 없습니다").setEphemeral(true).queue()
            return
        }

        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val cashFormatted = formatter.format(wallet.cash)
        val coinValue = wallet.totalWealth - wallet.cash
        val coinFormatted = formatter.format(coinValue)
        val totalFormatted = formatter.format(wallet.totalWealth)

        val message = """                                                                                                                        
              **${event.user.effectiveName}님의 재산**                                                                                             
              ━━━━━━━━━━━━━━━━━                                                                                                                    
              현금: ${cashFormatted}원                                                                                                             
              코인: ${coinFormatted}원                                                                                                             
              ━━━━━━━━━━━━━━━━━                                                                                                                    
              총 재산: ${totalFormatted}원                                                                                                         
          """.trimIndent()

        event.reply(message).queue()
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "내재산")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "내 재산 확인하기")
    }
}