package com.DiscordBot.KotlinDiscordBot.pubp.repository

import com.DiscordBot.KotlinDiscordBot.pubp.domain.PubgPlayers
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PubgRepository : JpaRepository<PubgPlayers, Long> {

    fun findByMember_UserId(userId: String): PubgPlayers
    fun existsByPlayerId(playerId: String): Boolean
}
