package com.DiscordBot.KotlinDiscordBot.pubp.service

import com.DiscordBot.KotlinDiscordBot.member.domain.Member
import com.DiscordBot.KotlinDiscordBot.member.repository.MemberRepository
import com.DiscordBot.KotlinDiscordBot.pubp.domain.PubgPlayers
import com.DiscordBot.KotlinDiscordBot.pubp.repository.PubgRepository
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
    @Value("\${pubg.api-key}") private val apiKey: String, private val memberRepository: MemberRepository
) {

    private val log = LoggerFactory.getLogger(PubgService::class.java)

    private val client = webClientBuilder
        .baseUrl("https://api.pubg.com")
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.api+json")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
        .build()
    fun getPlayersInfo(userId: String): String {
        log.info("getPlayersInfo() called. userId={}", userId)
        val player = pubgRepository.findByMember_UserId(userId)
            ?: throw IllegalArgumentException("PUBG player not registered for userId=$userId")

        log.info("getPlayersInfo() found mapped playerId={}", player.getPlayerId())
        return client.get()
            .uri { builder ->
                builder.path("/shards/steam/players")
                    .queryParam("filter[playerIds]", player.getPlayerId())
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

    @Transactional
    fun registrationPlayer(userId: String, playerId: String): PubgPlayers {
        log.info("registrationPlayer() called. userId={}, playerId={}", userId, playerId)
        if (pubgRepository.existByPlayerId(playerId)) {
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
}
