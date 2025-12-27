package com.DiscordBot.KotlinDiscordBot.member.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.member.data.MemberCreateDto
import com.DiscordBot.KotlinDiscordBot.member.service.MemberService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class MemberRegistrationCommand(
    private val memberService: MemberService
): SlashCommand {
    override val name: String = "registration"
    override val description: String = "member registration"

    override fun handle(event: SlashCommandInteractionEvent) {
        val user = event.user
        val member = event.member

        val userId = user.idLong
        val username = user.name
        val nickname = member?.effectiveName ?: username

        val memberCreateDto = MemberCreateDto(
            userId = userId.toString(),
            username = username,
            nickname = nickname
        )

        val savedMember = memberService.memberRegistration(memberCreateDto)

        if (savedMember == null) {
            event.reply("이미 등록했습니다").setEphemeral(true).queue()
            return
        }

        event.reply("등록에 성공했습니다.").setEphemeral(true).queue()
    }

    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "등록")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "멤버로 등록해 여러가지 기능을 즐겨보세요!")
    }
}