package com.DiscordBot.KotlinDiscordBot.money.dto

import com.DiscordBot.KotlinDiscordBot.coin.util.Market

data class PositionDto(
    val id: Long,
    val walletId: Long,
    val market: Market,
    val marketCount: Long,
    val cost: Long
)
