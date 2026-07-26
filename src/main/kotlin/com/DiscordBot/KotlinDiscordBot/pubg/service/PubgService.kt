package com.DiscordBot.KotlinDiscordBot.pubg.service

import com.DiscordBot.KotlinDiscordBot.member.domain.Member
import com.DiscordBot.KotlinDiscordBot.member.repository.MemberRepository
import com.DiscordBot.KotlinDiscordBot.pubg.data.PubgTeamMatchSummary
import com.DiscordBot.KotlinDiscordBot.pubg.domain.PubgPlayers
import com.DiscordBot.KotlinDiscordBot.pubg.repository.PubgRepository
import com.DiscordBot.KotlinDiscordBot.pubg.utils.PubgUtils
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.EntityExistsException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class PubgService(
    private val webClientBuilder: WebClient.Builder,
    private val pubgRepository: PubgRepository,
    @Value("\${pubg.api-key}") private val apiKey: String,
    private val memberRepository: MemberRepository,
    private val pubgUtils: PubgUtils
) {

    private val log = LoggerFactory.getLogger(PubgService::class.java)

    private val objectMapper = jacksonObjectMapper()

    private val client = webClientBuilder
        .baseUrl("https://api.pubg.com")
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.api+json")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
        .build()
    // 등록된 사용자의 PUBG 플레이어 ID로 플레이어 정보를 API에서 조회하는 함수입니다.
    fun getPlayersInfo(userId: String): String {
        log.info("getPlayersInfo() called. userId={}", userId)
        val player = pubgRepository.findByMember_UserId(userId)
            ?: throw IllegalArgumentException("PUBG player not registered for userId=$userId")

        log.info("getPlayersInfo() found mapped playerId={}", player.getPlayerId())
        return client.get()
            .uri { builder ->
                builder.path("/shards/steam/players")
                    .queryParam("filter[playerNames]", player.getPlayerId())
                    .build()
            }
            .retrieve()
            .onStatus(HttpStatusCode::isError) { res ->
                res.bodyToMono(String::class.java)
                    .flatMap { Mono.error(RuntimeException("PUBG API error ${res.statusCode()}: $it")) }
            }
            .bodyToMono(String::class.java)
            .block() ?: error("Empty response from PUBG API")
    }

    // PUBG 플레이어 이름으로 플레이어 정보를 API에서 조회하는 함수입니다.
    fun getPlayersByName(playerName: String): String {
        log.info("getPlayersByName() called. playerName={}", playerName)
        return client.get()
            .uri { builder ->
                builder.path("/shards/steam/players")
                    .queryParam("filter[playerNames]", playerName)
                    .build()
            }
            .retrieve()
            .onStatus(HttpStatusCode::isError) { res ->
                res.bodyToMono(String::class.java)
                    .flatMap { Mono.error(RuntimeException("PUBG API error ${res.statusCode()}: $it")) }
            }
            .bodyToMono(String::class.java)
            .block() ?: error("Empty response from PUBG API")
    }

    // PUBG 매치 ID로 매치 상세 정보를 API에서 조회하는 함수입니다.
    fun getPlayersMatchesInfo(matchId: String): String {
        log.info("getPlayersMatchesInfo() called. matchId={}", matchId)
        return client.get()
            .uri { builder ->
                builder.path("/shards/steam/matches/${matchId}")
                    .build()
            }
            .retrieve()
            .onStatus(HttpStatusCode::isError) { res ->
                res.bodyToMono(String::class.java)
                    .flatMap { Mono.error(RuntimeException("PUBG API error ${res.statusCode()}: $it")) }
            }
            .bodyToMono(String::class.java)
            .block() ?: error("Empty response from PUBG API")
    }

    // Discord 사용자와 PUBG 플레이어 ID를 연결해 저장하는 등록 함수입니다.
    @Transactional
    fun registrationPlayer(userId: String, playerId: String): PubgPlayers {
        log.info("registrationPlayer() called. userId={}, playerId={}", userId, playerId)
        if (pubgRepository.existsByPlayerId(playerId)) {
            log.warn("registrationPlayer() failed. duplicated playerId={}", playerId)
            throw EntityExistsException("playerId is exists")
        } else {
            val findMember = memberRepository.findByUserId(userId)
                ?: throw IllegalArgumentException("member not found: $userId")
            val conversionMember = Member(
                findMember.id,
                findMember.username,
                findMember.userId,
                findMember.nickname
            ).toEntity()
            val player = PubgPlayers.create(playerId = playerId, member = conversionMember)
            val saved = pubgRepository.save(player)
            log.info(
                "registrationPlayer() success. pubgPlayerId={}, memberId={}, playerId={}",
                saved.id, saved.member.id, saved.getPlayerId()
            )
            return saved
        }
    }

    fun getMyLatestTeamSummary(playerName: String): PubgTeamMatchSummary? {
        log.info("getMyLatestTeamSummary() called. playerName={}", playerName)
        val playerResponse = getPlayersByName(playerName)
        val playerRoot = objectMapper.readTree(playerResponse)
        val players = playerRoot.get("data")

        if (!players.isArray || players.isEmpty()) {
            throw IllegalArgumentException("Player not found: $playerName")
        }

        val matches = players
            .path(0)
            .path("relationships")
            .path("matches")
            .path("data")

        if (!matches.isArray || matches.isEmpty) {
            log.info(
                "Recent match not found. playerName={}",
                playerName
            )
            return null
        }

        val matchId = matches.path(0).path("id").asText()

        if (matchId.isBlank()) {
            log.warn(
                "Empty match ID. playerName={}",
                playerName
            )
            return null
        }

        val matchResponse = getPlayersMatchesInfo(matchId)
        log.info("matchResponse: $matchResponse")
        val matchRoot = objectMapper.readTree(matchResponse)
        log.info("matchRoot: $matchRoot")

        return pubgUtils.extractMyTeamSummary(matchRoot, playerName)
    }
}
