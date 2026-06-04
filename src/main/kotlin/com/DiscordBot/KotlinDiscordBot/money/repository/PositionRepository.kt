package com.DiscordBot.KotlinDiscordBot.money.repository

import com.DiscordBot.KotlinDiscordBot.coin.util.Market
import com.DiscordBot.KotlinDiscordBot.money.domain.Position
import com.DiscordBot.KotlinDiscordBot.money.domain.Wallet
import com.DiscordBot.KotlinDiscordBot.money.dto.PositionDto
import org.springframework.data.jpa.repository.JpaRepository

interface PositionRepository : JpaRepository<Position, Long> {
    // 지갑 ID와 마켓으로 단일 포지션을 조회하는 Spring Data JPA 파생 쿼리 함수입니다.
    fun findByWalletIdAndMarket(walletId: Long, market: Market): Position?
    // 지갑 ID에 속한 모든 포지션을 조회하는 Spring Data JPA 파생 쿼리 함수입니다.
    fun findByWalletId(walletId: Long): List<Position>
}
