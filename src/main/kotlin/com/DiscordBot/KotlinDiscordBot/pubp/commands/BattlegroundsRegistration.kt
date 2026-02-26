package com.DiscordBot.KotlinDiscordBot.pubp.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.member.service.MemberService
import com.DiscordBot.KotlinDiscordBot.pubp.service.PubgService
import jakarta.persistence.EntityExistsException
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BattlegroundsRegistration(
    private val memberService: MemberService,
    private val pubgService: PubgService
) : SlashCommand {
    private val log = LoggerFactory.getLogger(BattlegroundsRegistration::class.java)

    override val name: String = "pubgregistration"
    override val description: String = "pubg registration"

    override fun handle(event: SlashCommandInteractionEvent) {
        val inputPlayerId = event.getOption("playerid")?.asString?.trim()
        val userId = event.user.idLong.toString()
        log.info("pubgregistration command called. userId={}, inputPlayerId={}", userId, inputPlayerId)

        if (inputPlayerId.isNullOrBlank()) {
            log.warn("pubgregistration rejected. userId={}, reason=blank_player_id", userId)
            event.reply("Please enter a player ID.").setEphemeral(true).queue()
            return
        }

        if (!memberService.existsMember(userId)) {
            log.warn("pubgregistration rejected. userId={}, reason=member_not_found", userId)
            event.reply("Member not found. Please register first.").setEphemeral(true).queue()
            return
        }

        try {
            pubgService.registrationPlayer(userId, inputPlayerId)
            log.info("pubgregistration success. userId={}, playerId={}", userId, inputPlayerId)
            event.reply("PUBG player ID registered.").setEphemeral(true).queue()
        } catch (e: EntityExistsException) {
            log.warn("pubgregistration duplicated. userId={}, playerId={}", userId, inputPlayerId)
            event.reply("This player ID is already registered.").setEphemeral(true).queue()
        } catch (e: Exception) {
            log.error("pubgregistration failed. userId={}, playerId={}", userId, inputPlayerId, e)
            event.reply("Registration failed.").setEphemeral(true).queue()
        }
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "배그등록")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "배그 아이디를 등록하세요")
            .addOptions(
                OptionData(OptionType.STRING, "playerid", "PUBG player id", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "플레이어id")
                    .setDescriptionLocalization(DiscordLocale.KOREAN, "플레이어 아이디를 입력하세요")
            )
    }
}
