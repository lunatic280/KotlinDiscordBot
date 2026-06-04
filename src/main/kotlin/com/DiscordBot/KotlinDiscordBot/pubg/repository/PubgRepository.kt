package com.DiscordBot.KotlinDiscordBot.pubg.repository

import com.DiscordBot.KotlinDiscordBot.pubg.domain.PubgPlayers
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PubgRepository : JpaRepository<PubgPlayers, Long> {

    // 멤버의 Discord userId로 등록된 PUBG 플레이어를 조회하는 Spring Data JPA 파생 쿼리 함수입니다.
    fun findByMember_UserId(userId: String): PubgPlayers
    // PUBG 플레이어 ID의 중복 등록 여부를 조회하는 Spring Data JPA 파생 쿼리 함수입니다.
    fun existsByPlayerId(playerId: String): Boolean
}
