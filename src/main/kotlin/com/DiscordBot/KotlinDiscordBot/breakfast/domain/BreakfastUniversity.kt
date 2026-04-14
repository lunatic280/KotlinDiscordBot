package com.DiscordBot.KotlinDiscordBot.breakfast.domain

enum class BreakfastUniversity(
    val optionValue: String,
    val displayName: String
) {
    CATHOLIC("catholic", "Catholic University of Korea"),
    CNU("cnu", "Chungnam National University");

    companion object {
        fun fromOption(value: String): BreakfastUniversity? {
            return entries.firstOrNull { it.optionValue.equals(value.trim(), ignoreCase = true) }
        }
    }
}
