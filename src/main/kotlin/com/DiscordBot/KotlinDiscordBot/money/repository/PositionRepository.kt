package com.DiscordBot.KotlinDiscordBot.money.repository

import com.DiscordBot.KotlinDiscordBot.coin.util.Market
import com.DiscordBot.KotlinDiscordBot.money.domain.Position
import com.DiscordBot.KotlinDiscordBot.money.domain.Wallet
import org.springframework.data.jpa.repository.JpaRepository

interface PositionRepository : JpaRepository<Position, Long> {
    fun findByWalletIdAndMarket(walletId: Long, market: Market): Position?
}