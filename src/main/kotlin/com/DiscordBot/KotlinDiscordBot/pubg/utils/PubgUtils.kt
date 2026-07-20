package com.DiscordBot.KotlinDiscordBot.pubg.utils

import com.DiscordBot.KotlinDiscordBot.pubg.data.PubgTeamMatchSummary
import com.DiscordBot.KotlinDiscordBot.pubg.data.PubgTeamMemberSummary
import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.EmbedBuilder
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class PubgUtils {

    /**
     * PUBG 매치 상세 응답에서 특정 플레이어가 속한 팀의 경기 결과를 추출합니다.
     *
     * PUBG API의 `included` 배열에는 참가자(`participant`)와 팀(`roster`) 정보가
     * 함께 들어 있습니다. 참가자 ID를 기준으로 두 정보를 연결하여 대상 플레이어의
     * 팀을 찾고, 팀 순위와 팀원별 전투 기록을 [PubgTeamMatchSummary]로 변환합니다.
     *
     * @param matchRoot PUBG 매치 상세 API 응답의 최상위 JSON 노드
     * @param playerName 조회할 PUBG 플레이어 이름
     * @return 대상 플레이어가 속한 팀의 매치 요약
     * @throws NoSuchElementException 응답에서 대상 플레이어나 소속 팀을 찾지 못한 경우
     */
    fun extractMyTeamSummary(matchRoot: JsonNode, playerName: String): PubgTeamMatchSummary {
        // included에는 participant, roster 등 매치와 관련된 여러 리소스가 함께 들어 있습니다.
        val included = matchRoot["included"]

        // 참가자 ID로 참가자 JSON을 바로 찾을 수 있도록 Map으로 변환합니다.
        // roster에는 참가자 상세 정보 대신 참가자 ID만 있으므로 이후 연결에 사용합니다.
        val participantsById = included
            .filter { it["type"].asText() == "participant" }
            .associateBy { it["id"].asText() }

        // roster는 한 팀의 순위, 팀 ID, 소속 참가자 ID 목록을 담고 있습니다.
        val rosters = included
            .filter { it["type"].asText() == "roster" }

        // 모든 참가자 중 플레이어 이름이 요청한 이름과 같은 참가자를 찾습니다.
        val targetParticipant = participantsById.values.first {
            it["attributes"]["stats"]["name"].asText() == playerName
        }

        val targetParticipantId = targetParticipant["id"].asText()

        // 참가자 관계 목록에 대상 플레이어 ID가 포함된 roster가 플레이어의 소속 팀입니다.
        val myRoster = rosters.first { roster ->
            roster["relationships"]["participants"]["data"].any { participantRef ->
                participantRef["id"].asText() == targetParticipantId
            }
        }

        // 소속 팀의 참가자 ID를 participantsById와 연결하여 팀원 상세 정보를 가져옵니다.
        val myTeamMembers = myRoster["relationships"]["participants"]["data"]
            .map { it["id"].asText() }
            .mapNotNull { participantsById[it] }

        // PUBG API의 참가자 JSON에서 화면에 사용할 이름, 피해량, 처치 수만 추출합니다.
        val members = myTeamMembers.map { participant ->
            val stats = participant["attributes"]["stats"]
            PubgTeamMemberSummary(
                name = stats["name"].asText(),
                damageDealt = stats["damageDealt"].asDouble(),
                kills = stats["kills"].asInt()
            )
        }

        // 팀원별 피해량을 합산하고 매치·팀 정보를 묶어 최종 결과를 반환합니다.
        return PubgTeamMatchSummary(
            matchId = matchRoot["data"]["id"].asText(),
            teamId = myRoster["attributes"]["stats"]["teamId"].asInt(),
            rank = myRoster["attributes"]["stats"]["rank"].asInt(),
            totalDamage = members.sumOf { it.damageDealt },
            members = members
        )
    }

    fun makeGraphEmbed(result: PubgTeamMatchSummary): EmbedBuilder {
        val embed = EmbedBuilder()
            .setColor(Color.BLUE)
            .setTitle("최근 매치 정보")
            .setDescription("최근 매치 결과입니다.")
            .addField("팀 순위: ", result.rank.toString(), true)
            .addField("팀 전체 데미지: ", result.totalDamage.toInt().toString(), true)

        result.members
            .sortedByDescending{ it.damageDealt }
            .forEach { member ->
                embed.addField(
                    member.name,
                    """
                피해량: ${"%.1f".format(member.damageDealt)}
                킬: ${member.kills}
                """.trimIndent(),
                    false
                )
            }
        return embed
    }
}
