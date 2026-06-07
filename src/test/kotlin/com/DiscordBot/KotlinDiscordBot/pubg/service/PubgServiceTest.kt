package com.DiscordBot.KotlinDiscordBot.pubg.service

import com.DiscordBot.KotlinDiscordBot.member.repository.MemberRepository
import com.DiscordBot.KotlinDiscordBot.pubg.repository.PubgRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito
import org.springframework.web.reactive.function.client.WebClient

@Tag("live")
@EnabledIfEnvironmentVariable(named = "PUBG_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PUBG_TEST_PLAYER_NAME", matches = ".+")
class PubgServiceTest {

    private val objectMapper = jacksonObjectMapper()
    private val pubgRepository = Mockito.mock(PubgRepository::class.java)
    private val memberRepository = Mockito.mock(MemberRepository::class.java)

    private fun createService(): PubgService {
        return PubgService(
            webClientBuilder = WebClient.builder(),
            pubgRepository = pubgRepository,
            apiKey = System.getenv("PUBG_API_KEY"),
            memberRepository = memberRepository
        )
    }

    @Test
    fun `getPlayersByName 함수가  players를 받아오는지 확인`() {
        val playerName = System.getenv("PUBG_TEST_PLAYER_NAME")

        val response = createService().getPlayersByName(playerName)
        val root = objectMapper.readTree(response)
        //println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root))

        assertThat(root["data"]).isNotNull()
        assertThat(root["data"].isArray).isTrue()
        assertThat(root["data"].size()).isGreaterThan(0)

        val player = root["data"][0]
        assertThat(player["type"].asText()).isEqualTo("player")
        assertThat(player["id"].asText()).isNotBlank()
        assertThat(player["attributes"]["name"].asText()).isEqualTo(playerName)
    }

    @Test
    fun `getPlayersMatchesInfo를 이용해서 최근 매치 정보 가져오기`() {
        val playerName = System.getenv("PUBG_TEST_PLAYER_NAME")


        val playerResponse = createService().getPlayersByName(playerName)
        val playerRoot = objectMapper.readTree(playerResponse)
        val matches = playerRoot["data"][0]["relationships"]["matches"]["data"]
        //println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(matches))

        assumeTrue(
            matches.isArray && matches.size() > 0,
            "최근 매치가 없는 플레이어라서 match test를 skip합니다."
        )

        val matchId = matches[0]["id"].asText()
        val matchResponse = createService().getPlayersMatchesInfo(matchId)
        val matchRoot = objectMapper.readTree(matchResponse)
        //println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(matchRoot))

        assertThat(matchRoot["data"]["type"].asText()).isEqualTo("match")
        assertThat(matchRoot["data"]["id"].asText()).isEqualTo(matchId)
        assertThat(matchRoot["included"].isArray).isTrue()
        assertThat(matchRoot["included"].size()).isGreaterThan(0)
    }

    @Test
    fun `매치로그에서 자신의 정보와 팀원 정보 가져오기`() {
        val playerName = System.getenv("PUBG_TEST_PLAYER_NAME")

        val playerResponse = createService().getPlayersByName(playerName)
        val playerRoot = objectMapper.readTree(playerResponse)
        val matches = playerRoot["data"][0]["relationships"]["matches"]["data"]

        assumeTrue(
            matches.isArray && matches.size() > 0,
            "최근 매치가 없는 플레이어라서 match test를 skip합니다."
        )

        val matchId = matches[0]["id"].asText()
        val matchResponse = createService().getPlayersMatchesInfo(matchId)
        val matchRoot = objectMapper.readTree(matchResponse)

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

        val myTeamParticipantIds = myRoster["relationships"]["participants"]["data"]
            .map { it["id"].asText() }

        val myTeamMembers = myTeamParticipantIds.mapNotNull { participantsById[it]}

        val myTeamDamage = myTeamMembers.sumOf {
            it["attributes"]["stats"]["damageDealt"].asDouble()
        }

        println("내 팀 ID: ${myRoster["attributes"]["stats"]["teamId"].asInt()}")
        println("내 팀 순위: ${myRoster["attributes"]["stats"]["rank"].asInt()}")
        println("내 팀 총 딜량: $myTeamDamage")

        myTeamMembers.forEach { participant ->
            val stats = participant["attributes"]["stats"]
            println("${stats["name"].asText()} - damage=${stats["damageDealt"].asDouble()}")
        }
    }
}
