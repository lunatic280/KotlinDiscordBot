package com.DiscordBot.KotlinDiscordBot.pubg.utils

import com.DiscordBot.KotlinDiscordBot.pubg.data.PubgTeamMatchSummary
import com.DiscordBot.KotlinDiscordBot.pubg.data.PubgTeamMemberSummary
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component

@Component
class PubgUtils {

    fun extractMyTeamSummary(matchRoot: JsonNode, playerName: String): PubgTeamMatchSummary {
        val included = matchRoot["included"]

        val participantsById = included
            .filter { it["type"].asText() == "participant" }
            .associateBy { it["id"].asText() }

        val rosters = included
            .filter { it["type"].asText() == "roster" }

        val targetParticipant = participantsById.values.first {
            it["attributes"]["stats"]["name"].asText() == playerName
        }

        val targetParticipantId = targetParticipant["id"].asText()

        val myRoster = rosters.first { roster ->
            roster["relationships"]["participants"]["data"].any { participantRef ->
                participantRef["id"].asText() == targetParticipantId
            }
        }

        val myTeamMembers = myRoster["relationships"]["participants"]["data"]
            .map { it["id"].asText() }
            .mapNotNull { participantsById[it] }

        val members = myTeamMembers.map { participant ->
            val stats = participant["attributes"]["stats"]
            PubgTeamMemberSummary(
                name = stats["name"].asText(),
                damageDealt = stats["damageDealt"].asDouble(),
                kills = stats["kills"].asInt()
            )
        }

        return PubgTeamMatchSummary(
            matchId = matchRoot["data"]["id"].asText(),
            teamId = myRoster["attributes"]["stats"]["teamId"].asInt(),
            rank = myRoster["attributes"]["stats"]["rank"].asInt(),
            totalDamage = members.sumOf { it.damageDealt },
            members = members
        )
    }
}
