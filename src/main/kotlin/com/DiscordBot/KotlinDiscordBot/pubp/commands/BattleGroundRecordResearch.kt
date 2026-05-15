package com.DiscordBot.KotlinDiscordBot.pubp.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.member.service.MemberService
import com.DiscordBot.KotlinDiscordBot.pubp.service.PubgService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BattleGroundRecordResearch(
    private val memberService: MemberService,
    private val pubgService: PubgService
): SlashCommand {
    private val log = LoggerFactory.getLogger(BattleGroundRecordResearch::class.java)
    override val name: String = "battlegroundrecordresearch"
    override val description: String = "research match information"

    // 배틀그라운드 최근 매치 조회 명령의 등록 여부를 확인하고 현재 구현 상태를 안내하는 함수입니다.
    override fun handle(event: SlashCommandInteractionEvent) {
        val userId = event.user.idLong
        val existUser = memberService.existsMember(userId.toString())
        if (!existUser) {
            log.warn("member is not exist")
            event.reply("등록이 필요합니다").setEphemeral(true).queue()
        } else {
            log.info("BattleGroundRecordResearch is not implemented yet for userId={}", userId)
            event.reply("최근 매치 조회 기능은 아직 구현 중입니다.").setEphemeral(true).queue()
        }
    }

    // 배틀그라운드 매치 검색 슬래시 명령의 이름과 설명을 생성하는 함수입니다.
    override fun getCommandData(): SlashCommandData {
        return Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "매치검색")
            .setDescriptionLocalization(DiscordLocale.KOREAN, "최근 매치 정보를 가져옵니다.")
    }
}
