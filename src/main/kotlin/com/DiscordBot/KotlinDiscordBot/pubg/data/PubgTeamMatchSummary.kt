package com.DiscordBot.KotlinDiscordBot.pubg.data

data class PubgTeamMatchSummary(
    val matchId: String,
    val teamId: Int,
    val rank: Int,
    val totalDamage: Double,
    val members: List<PubgTeamMemberSummary>
)


