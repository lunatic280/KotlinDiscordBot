package com.DiscordBot.KotlinDiscordBot.money.dto

import com.DiscordBot.KotlinDiscordBot.coin.util.Market

data class PositionMarketDto(
    val market: Market,
    val marketCount: Long
)
