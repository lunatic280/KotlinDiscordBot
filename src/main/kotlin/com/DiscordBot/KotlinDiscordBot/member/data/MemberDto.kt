package com.DiscordBot.KotlinDiscordBot.member.data

import java.time.LocalDate

data class MemberDto(
    val id: Long? = null,
    val username: String,
    val userId: String,
    val nickname: String,
    val money: Long,
    val level: Long,
    val lastDailyReward: LocalDate?
)
