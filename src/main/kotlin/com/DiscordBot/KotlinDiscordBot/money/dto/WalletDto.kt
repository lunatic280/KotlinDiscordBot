package com.DiscordBot.KotlinDiscordBot.money.dto

data class WalletDto(
    val id: Long? = null,
    val memberId: String,
    val cash: Long
)
