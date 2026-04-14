package com.DiscordBot.KotlinDiscordBot.breakfast.domain

import java.time.LocalDate

data class BreakfastInfo(
    val university: BreakfastUniversity,
    val date: LocalDate,
    val time: String,
    val location: String,
    val price: String,
    val menuItems: List<String>,
    val sourceUrls: List<String>,
    val note: String? = null
)
